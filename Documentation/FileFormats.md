# Serenity Formats

This document summarizes all non-standard file formats, data formats, and mime-types used by Serenity.

## Clipboard

The clipboard feature works through the Clipboard server, which generally acts as a global storage or three things:
- a `String` mime type,
- a (potentially large) block of data, shared as an anonymous file,
- a `HashMap<String, String>` of arbitrary metadata, depending on the mime type.

See also [`Userland/Libraries/LibGUI/Clipboard.h`](../Userland/Libraries/LibGUI/Clipboard.h).

## Drag & drop

In contrast to the clipboard, the drag & drop feature works through WindowServer, and a bouquet of data is transmitted:
- a `[UTF8] String` to be displayed while dragging,
- a `HashMap<String, ByteBuffer>` map that contains arbitrary data for a variety of possible mime types,
- a `Gfx::ShareableBitmap` to be displayed while dragging

Drag & drop is most prominently supported by File Manager, Spreadsheet, and Terminal.
Various applications accept drag & drop to open files.

## glyph/x-fonteditor (Clipboard-only)

Requires the metadata-fields, `char` (usually encoded as a string of length 1: the codepoint as a character),
`width`, and `height` (encoded as a decimal number).

The glyph bitmap data is encoded in width times height many bytes, either 0 (clear) or 1 (set).

Implemented in `GlyphEditorWidget::copy_glyph` and `GlyphEditorWidget::paste_glyph`, in [`Userland/Applications/FontEditor/GlyphEditorWidget.cpp`](../Userland/Applications/FontEditor/GlyphEditorWidget.cpp).

## image/x-serenityos (Clipboard-only)

Requires the metadata-fields `width`, `height`, `scale`, `format` (see `Gfx::BitmapFormat`), and `pitch`, encoded as decimal strings.

The data is encoded according to `Gfx::determine_storage_format(BitmapFormat)`, so either as
BGRx8888, BGRA8888, RGBA8888, or 8-bit palette index. Note that the palette is not transferred.

Implemented in [`Clipboard::set_bitmap` and `Clipboard::DataAndType::as_bitmap()`](../Userland/Libraries/LibGUI/Clipboard.cpp).

## text/uri-list (Clipboard and drag&drop)

Newline-delimited set of URIs. Used by File Manager, FileSystemModel, and Terminal.

Example:

```
file:///home/anon/Desktop/Browser
file:///home/anon/Desktop/Help
file:///home/anon/Desktop/Home
```

## Application File (`*.af` files)

These files are human-readable and are a subset of the INI-format, have no easily detectable filemagic.
These files define System Menu entries and launcher file types / protocols.

They are stored in [`/res/apps`](../Base/res/apps).

See also [`Userland/Services/Taskbar/main.cpp`](../Userland/Services/Taskbar/main.cpp) and `Launcher::load_handlers` in [`Userland/Services/LaunchServer/Launcher.cpp`](../Userland/Services/LaunchServer/Launcher.cpp).

## Font (`*.font` files)

These files contain bitmap definitions of fonts, either varying-width or fixed-width.
The header definition can be found in `Gfx::FontFileHeader` in [`Userland/Libraries/LibGfx/BitmapFont.cpp`](../Userland/Libraries/LibGfx/BitmapFont.cpp).
Most prominently, the first four bytes contain the filemagic: `!Fnt`.

## GUI Markup Language (`*.gml` files)

These files are human-readable, have no easily detectable filemagic, and define GUI interfaces.
The format is strongly influenced by QML, the Qt Modeling Language.

See also [Playground(1)](../Userland/DevTools/Playground/) and `Widget::load_from_gml` in [`Userland/Libraries/LibGUI/Widget.cpp`](../Userland/Libraries/LibGUI/Widget.cpp).

## Inter Process Communication (`*.ipc` files)

These files are human-readable, have no easily detectable filemagic, and define IPC interfaces.
The format is loosely inspired by C++ headers.

See also [`Meta/Lagom/Tools/CodeGenerators/IPCCompiler/`](../Meta/Lagom/Tools/CodeGenerators/IPCCompiler/).

## Inter Process Communication (through Unix sockets)

The various services each have their own formats, all very similar, and automatically implemented through LibIPC. The specifics depend on the corresponding source `.ipc` file.

The format can be identified by the format magic, which is derived in [`Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp`](../Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp)
from the service-endpoint name, e.g. "ClipboardClient" (which hashes to 4008793515) or "ClipboardServer" (which hashes to 1329211611).

In general, communication works by packets, which might have been sent in response to other packets. Everything is host endianness. Each packet consists of:
- a 32-bit message size (see `Connection::try_parse_messages` in [`Userland/Libraries/LibIPC/Connection.h`](../Userland/Libraries/LibIPC/Connection.h))
- the 32-bit endpoint magic (note that responses use the endpoint of the requesting packet, so the Clipboard server might use the endpoint magic 4008793515 to signal that this packet is a response)
- the 32-bit message ID within that endpoint (sequentially assigned, starting at 1)
- the data of that message itself (e.g. see `Messages::ClipboardServer::SetClipboardData::{en,de}code` in `Build/*/Userland/Services/Clipboard/ClipboardServerEndpoint.h`).

## Postcreate (`*.postcreate` files)

Shell-script executed by HackStudio after creating a new project.

See also `ProjectTemplate::create_project` in [`Userland/DevTools/HackStudio/ProjectTemplate.cpp`](../Userland/DevTools/HackStudio/ProjectTemplate.cpp).
