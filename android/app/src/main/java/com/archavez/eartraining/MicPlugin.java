package com.archavez.eartraining;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Mic")
public class MicPlugin extends Plugin {
    private static final int SAMPLE_RATE = 44100;
    private static final int READ_SIZE = 2048;

    private AudioRecord recorder;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private volatile double lastFrequency = -1;

    @PluginMethod
    public void start(PluginCall call) {
        if (isRecording) { call.resolve(); return; }
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        int bufSize = Math.max(minBuf, READ_SIZE * 4);
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT, bufSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                call.reject("AudioRecord init failed");
                return;
            }
            isRecording = true;
            recorder.startRecording();
            final float[] buf = new float[READ_SIZE];
            recordingThread = new Thread(() -> {
                while (isRecording) {
                    int n = recorder.read(buf, 0, READ_SIZE, AudioRecord.READ_BLOCKING);
                    if (n > 0) lastFrequency = autocorrelate(buf, n);
                }
            });
            recordingThread.setDaemon(true);
            recordingThread.start();
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        isRecording = false;
        lastFrequency = -1;
        if (recorder != null) {
            try { recorder.stop(); recorder.release(); } catch (Exception ignored) {}
            recorder = null;
        }
        if (call != null) call.resolve();
    }

    @PluginMethod
    public void getPitch(PluginCall call) {
        JSObject r = new JSObject();
        r.put("frequency", lastFrequency);
        call.resolve(r);
    }

    private double autocorrelate(float[] buf, int n) {
        double sum2 = 0;
        for (int i = 0; i < n; i++) sum2 += buf[i] * buf[i];
        double rms = Math.sqrt(sum2 / n);
        if (rms < 0.005) return -1; // very quiet = silence
        // Voice frequency range: 80 Hz (low bass) to 1050 Hz (high soprano)
        int minP = SAMPLE_RATE / 1050;
        int maxP = Math.min(SAMPLE_RATE / 80, n / 2);
        double best = -1;
        int bestP = -1;
        for (int p = minP; p < maxP; p++) {
            double c = 0;
            for (int i = 0; i < n - p; i++) c += buf[i] * buf[i + p];
            c /= (n - p);
            if (c > best) { best = c; bestP = p; }
        }
        // Relative threshold: correlation must be at least 15% of signal energy
        double threshold = 0.15 * (sum2 / n);
        return (best < threshold || bestP < 0) ? -1 : (double) SAMPLE_RATE / bestP;
    }
}
