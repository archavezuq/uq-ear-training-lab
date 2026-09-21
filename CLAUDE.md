# Audikus — Entrenamiento Auditivo Musical

App de ear training para Android. Vanilla JS puro envuelto con Capacitor 6 para Play Store.

## Stack

- **Sin framework** — toda la app es un único archivo HTML autocontenido
- **Tone.js v14** — audio (sampler + fallback PolySynth), cargado vía CDN
- **Capacitor 6** — wrapper Android nativo
- **localStorage** — toda la persistencia, sin backend
- **CSS custom properties** — sistema de estilos, dark mode exclusivo

## Archivos críticos

| Archivo | Propósito |
|---|---|
| `www/index.html` | App completa — Capacitor/Play Store (4147 líneas) |
| `index.html` (raíz) | Demo GitHub Pages (1446 líneas, layout desktop) |
| `android/app/build.gradle` | versionCode — incrementar en cada build |
| `android/keystore.properties` | Contraseñas del keystore — local, NO está en git |
| `capacitor.config.json` | `{"appId":"com.archavez.eartraining","webDir":"www"}` |

**IMPORTANTE:** `www/index.html` y `index.html` (raíz) son archivos separados. Cualquier cambio de funcionalidad debe aplicarse a ambos manualmente.

## Reglas de build

- Siempre incrementar `versionCode` en `android/app/build.gradle` antes de cada build para Play Store — Google rechaza versionCodes repetidos
- Build: `npx cap sync` → `cd android` → `./gradlew bundleRelease`
- El keystore está en `keystore/ear-training.keystore` (ignorado por git). Las contraseñas están en `android/keystore.properties` (también ignorado por git, solo existe en la máquina local)

## Estructura de la app (www/index.html)

Todo el código está en un único bloque `<script>` al final del `<body>`. El orden interno es:

1. Variables globales y configuración (`tuningA`, `currentLang`, `scores`, etc.)
2. Objeto `ui` — strings bilingües ES/EN (~100+ keys)
3. Objeto `musicData` — datos de todos los módulos (intervalos, acordes, escalas, progresiones)
4. Funciones de audio (`initAudio`, `getFreq`, `synth`, `playSound`)
5. Funciones de ejercicio (`nextQuestion`, `checkAnswer`, `renderOptions`)
6. Funciones de progreso (`recordAnswer`, `updateStreakForToday`, `renderProgressPanel`)
7. Funciones de UI (`showSinko`, `showComboLabel`, `setLang`, `updateStaticText`)
8. Inicialización (`handleStart`, `checkLastStudy`)

## Módulos de entrenamiento existentes

`intervalos`, `triadas`, `inversiones`, `septimas`, `progresiones`, `escalas`, `notas`, `alturas`, `entonacion`, `entonAcordes`, `entonEscalas`

Los módulos de entonación usan micrófono nativo (`window.Capacitor.Plugins.Mic`) — solo funcionan en el APK, no en el demo web.

## Sistema de audio

- Sampler (Salamander piano, 22 samples MP3 en `www/audio/piano/`) + fallback `PolySynth`
- El sampler se carga lazy después del primer toque del usuario
- Tuning configurable: 440 o 442 Hz (`tuningA`)
- `studyTimers` — array de setTimeout que se limpia antes de cada reproducción nueva

## Colores de marca

```css
--bg: #0f0f1e
--primary: #6c63ff    /* violeta principal */
--success: #06d6a0    /* verde — correcto */
--danger: #ef476f     /* rojo — incorrecto */
--warning: #fbbf24    /* amarillo — aproximación */
```

## Sistema bilingüe

`currentLang` ('es' / 'en') — función `tx(key)` para acceder al objeto `ui`. Función `dn(name)` para términos musicales. Cambio sincrónico sin recargar la página. Cualquier texto nuevo debe agregarse en ambos idiomas en el objeto `ui`.

## Checklist antes de cada build

- [ ] Buscar variables declaradas pero no usadas (`let`, `const`, `var` huérfanas)
- [ ] Buscar funciones definidas pero nunca llamadas
- [ ] Buscar assets en `www/audio/` e `www/img/` que no se referencien en el código
- [ ] Verificar que `versionCode` en `android/app/build.gradle` fue incrementado
- [ ] Confirmar que `android/keystore.properties` existe en la máquina local
- [ ] Hacer `npx cap sync` antes de `./gradlew bundleRelease`
- [ ] Probar el APK en dispositivo antes de subir a Play Console

## Convenciones al modificar el código

- Un cambio a la vez — el archivo es grande y único. Terminar, hacer commit y push antes de empezar el siguiente cambio
- Al agregar un módulo nuevo: agregarlo al objeto `musicData`, al array `modes` en `renderModeCards()`, y a ambos objetos de idioma en `ui.es` y `ui.en`
- Voicings de acordes: usar posición abierta (3-7-9, no 1-3-5-7-9) para que las extensiones sean audibles
- No agregar dependencias externas nuevas sin evaluar el impacto en el tamaño del APK

## Repositorio

`https://github.com/archavezuq/uq-ear-training-lab`  
Rama principal: `main`
