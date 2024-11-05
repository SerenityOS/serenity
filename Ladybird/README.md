# Ladybird

> [!NOTE]
> The Ladybird web browser project has moved to [LadybirdBrowser/ladybird](https://github.com/LadybirdBrowser/ladybird), this version is kept as a developer convenience for the testing of LibWeb and LibJS libraries included with SerenityOS.

Ladybird is a web browser built on the [LibWeb](https://github.com/SerenityOS/serenity/tree/master/Userland/Libraries/LibWeb) and [LibJS](https://github.com/SerenityOS/serenity/tree/master/Userland/Libraries/LibJS) engines from [SerenityOS](https://github.com/SerenityOS/serenity).
The Browser UI has a cross-platform GUI in Qt6 and a macOS-specific GUI in AppKit.

Ladybird aims to be a standards-compliant, independent web browser with no third-party dependencies.
Currently, the only dependencies are UI frameworks like Qt6 and AppKit, and low-level platform-specific
libraries like PulseAudio, CoreAudio and OpenGL.

## Features

The Ladybird browser application uses a multiprocess architecture with a main UI process, several WebContent renderer processes,
an ImageDecoder process, a RequestServer process, and a SQLServer process for holding cookies.

Image decoding and network connections are done out of process to be more robust against malicious content.
Each tab has its own renderer process, which is sandboxed from the rest of the system.

All the core library support components are developed in the serenity monorepo:

-   LibWeb: Web Rendering Engine
-   LibJS: JavaScript Engine
-   LibWasm: WebAssembly implementation
-   LibCrypto/LibTLS: Cryptography primitives and Transport Layer Security (rather than OpenSSL)
-   LibHTTP: HTTP/1.1 client
-   LibGfx: 2D Graphics Library, Image Decoding and Rendering (rather than skia)
-   LibArchive: Archive file format support (rather than libarchive, zlib)
-   LibUnicode, LibLocale: Unicode and Locale support (rather than libicu)
-   LibAudio, LibVideo: Audio and Video playback (rather than libav, ffmpeg)
-   LibCore: Event Loop, OS Abstraction layer
-   LibIPC: Inter-Process Communication
-   ... and more!

## Building and Development

See [build instructions](../Documentation/BuildInstructionsLadybird.md) for information on how to build Ladybird.

See [CONTRIBUTING.md](../CONTRIBUTING.md) for information on how to contribute to Ladybird.

## More Information

For more information about the history of Ladybird, see [this blog post](https://awesomekling.github.io/Ladybird-a-new-cross-platform-browser-project/).

The official website for Ladybird is [ladybird.dev](https://ladybird.dev).
