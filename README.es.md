# 🏠 CASERO.cu
App multiplataforma (iOS + Android) para que los arrendadores de casas particulares en Cuba reporten sus huéspedes a las autoridades. 🇨🇺

[![Licencia: MIT](https://img.shields.io/badge/Licencia-MIT-yellow.svg)](LICENSE)
[![Plataformas: iOS | Android](https://img.shields.io/badge/Plataformas-iOS%20%7C%20Android-555555)](#-stack-tecnológico)
[![Lenguaje: Kotlin](https://img.shields.io/badge/Lenguaje-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![UI: Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?logo=jetpackcompose)](https://www.jetbrains.com/compose-multiplatform/)
[![PRs: Bienvenidos](https://img.shields.io/badge/PRs-bienvenidos-brightgreen)](CONTRIBUTING.md)

[See the English version](README.md)

## ⚠️ Aviso

> [!WARNING]
> **Proyecto no oficial.** No está afiliado a CIDP-MININT ni a ninguna entidad gubernamental. Se conecta al portal oficial con las credenciales del propio arrendador, igual que un navegador. No nos hacemos responsables de cambios en `casero.rem.cu`, en los códigos USSD/SMS, ni de problemas derivados del uso de este software. Úselo bajo su propio riesgo. Verifique siempre los registros por los canales oficiales.

## 🚧 Estado

Port completo, todavía sin publicar. Este repositorio sustituye a los clientes nativos
[casero.cu-ios](https://github.com/albertolicea00/casero.cu-ios) (Swift, completo)
y [casero.cu-apk](https://github.com/albertolicea00/casero.cu-apk) (Kotlin, parcial)
con un único código en Kotlin Multiplatform. La app de iOS es la referencia: el
port debe igualar sus funciones y pasar sus tests antes de la primera versión.

## ✨ Funciones

- Inicio de sesión en el portal con certificate pinning acotado, cookies y tokens
  antifalsificación.
- Búsqueda de huéspedes por pasaporte, nombre, CI y CI temporal; registro por lotes
  con revisión previa, ID de estancia en el resultado, PDF (antes, después y si
  falla) y conservación de registros sin confirmar. Vea el
  [contrato de registro](AGENTS.md#registration--confirmed).
- Listas de huéspedes con todas las páginas, selección de fechas, búsqueda, filtros
  activos/historial y acceso sin conexión a los datos guardados.
- Detalle del huésped, caché opcional de fotos, acompañantes y cambio de salida.
- Perfil del portal y teléfonos/correos registrados solo para consulta; los cambios
  abren la página oficial del portal.
- Ayuda dentro de la app: la ayuda oficial del portal y un resumen sin conexión de
  la guía de Cubatrámite.
- Exportaciones CSV/Excel y PDF.
- Credenciales guardadas solo en el dispositivo, recuperación de sesión con
  biometría/código y borrado de datos locales al cerrar sesión. Vea
  [SECURITY.md](SECURITY.md#saved-credentials-and-automatic-sign-in).
- Reporte por SMS al **7344** con el prefijo **HC**, sin iniciar sesión en el
  portal. Vea el [contrato de SMS](AGENTS.md#sms-reporting-contract).
- Interfaz en español e inglés. La política de privacidad, los términos y el EULA
  están en el [sitio web](https://casero-cu.vercel.app/privacy.html).

## 🛠 Stack Tecnológico

| Capa | Opción |
|------|--------|
| Lenguaje | Kotlin |
| Código compartido | Kotlin Multiplatform |
| UI | Compose Multiplatform |
| Plataformas | Android, iOS 16+ |
| Red | Portal ASP.NET MVC (token antifalsificación + cookies de sesión) |

## 🏗 ¿Por qué un solo código?

Los clientes nativos de iOS y Android hacían lo mismo y cada cambio del portal había
que arreglarlo dos veces. Kotlin Multiplatform mantiene un solo código para la lógica
y la interfaz, y aun así compila a código nativo en cada plataforma. Lo que necesita
al sistema operativo (certificate pinning, almacenamiento seguro de credenciales,
biometría, SMS, USSD en Android) son módulos pequeños por plataforma detrás de una
interfaz común, así que Android conserva la integración más profunda que permite.
Vea [ARCHITECTURE.md](ARCHITECTURE.md).

## 🚀 Primeros Pasos

Necesita JDK 17, el SDK de Android y, para iOS, una Mac con Xcode y XcodeGen
(`brew install xcodegen`).

```bash
git clone https://github.com/albertolicea00/casero.cu.git
cd casero.cu
make                      # lista todas las tareas
make run                  # Android (dispositivo o emulador)
make run PLATFORM=ios     # simulador de iOS
make test                 # tests compartidos en la JVM y en el simulador de iOS
```

Android Studio o IntelliJ IDEA con el plugin de Kotlin Multiplatform abren el
proyecto directamente. Para Xcode, ejecute `make ios-project` y abra
`iosApp/CaseroCu.xcodeproj`.

> 🔐 El material de firma (`*.jks`, `*.keystore`, `*.p12`, `*.mobileprovision`, `*.p8`) nunca se sube al repositorio.

## 🔒 Nota sobre TLS

El portal sirve un certificado que no pasa la validación estándar. La app fija ese
certificado solo para `casero.rem.cu`; la validación TLS nunca se desactiva de forma
global. Vea [AGENTS.md](AGENTS.md#portal-api-reference) para el flujo de peticiones
obtenido por ingeniería inversa.

## 🤝 Contribuir

Vea [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md) y [Código de Conducta](CODE_OF_CONDUCT.md). Usamos [Conventional Commits](https://www.conventionalcommits.org/).

### 🐛 Reportar Bugs

Abra un issue en [este repositorio](https://github.com/albertolicea00/casero.cu/issues).
Los repositorios nativos están archivados y ya no aceptan issues.

## 📦 Repos Relacionados

- [casero.cu-web](https://github.com/albertolicea00/casero.cu-web) — Sitio web
- [casero.cu-ios](https://github.com/albertolicea00/casero.cu-ios) — Cliente nativo iOS (archivado)
- [casero.cu-apk](https://github.com/albertolicea00/casero.cu-apk) — Cliente nativo Android (archivado)

## 📄 Licencia

[MIT](LICENSE) © 2026 Alberto Licea
