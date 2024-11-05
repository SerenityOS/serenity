## Name

clipboard - Data formats specific to Clipboard and drag & drop

## Clipboard

The clipboard feature works through the Clipboard server, which generally acts as a global storage or three things:

-   a `ByteString` mime type,
-   a (potentially large) block of data, shared as an anonymous file,
-   a `HashMap<ByteString, ByteString>` of arbitrary metadata, depending on the mime type.

See also [`Userland/Libraries/LibGUI/Clipboard.h`](../../../../../Userland/Libraries/LibGUI/Clipboard.h).

## Drag & drop

In contrast to the clipboard, the drag & drop feature works through WindowServer, and a bouquet of data is transmitted:

-   a `[UTF8] ByteString` to be displayed while dragging,
-   a `HashMap<ByteString, ByteBuffer>` map that contains arbitrary data for a variety of possible mime types,
-   a `Gfx::ShareableBitmap` to be displayed while dragging

Drag & drop is most prominently supported by File Manager, Spreadsheet, and Terminal.
Various applications accept drag & drop to open files.

## glyph/x-fonteditor (Clipboard-only)

Requires the metadata-fields `count` (count of glyphs copied) and `first_glyph` (lowest codepoint that is copied), encoded as decimal strings.

The data contains code point (encoded as host-endian `u32`), width and height (as `u8`'s) and glyph bitmap data. It is encoded in width times height many bytes, either 0 (clear) or 1 (set).

Implemented in `FontEditor::MainWidget::copy_selected_glyphs` and `FontEditor::MainWidget::paste_glyphs`, in [`Userland/Applications/FontEditor/MainWidget.cpp`](../../../../../Userland/Applications/FontEditor/MainWidget.cpp).

## image/x-serenityos (Clipboard-only)

Requires the metadata-fields `width`, `height`, `scale`, `format` (see `Gfx::BitmapFormat`), and `pitch`, encoded as decimal strings.

The data is encoded according to `Gfx::determine_storage_format(BitmapFormat)`, so either as
`BGRx8888`, `BGRA8888`, `RGBA8888`, or 8-bit palette index. Note that the palette is not transferred.

Implemented in [`Clipboard::set_bitmap` and `Clipboard::DataAndType::as_bitmap()`](../../../../../Userland/Libraries/LibGUI/Clipboard.cpp).

## text/uri-list (Clipboard and drag & drop)

Newline-delimited set of URIs. Used by File Manager, `FileSystemModel`, and Terminal.

Example:

```
file:///home/anon/Desktop/Browser
file:///home/anon/Desktop/Help
file:///home/anon/Desktop/Home
```
