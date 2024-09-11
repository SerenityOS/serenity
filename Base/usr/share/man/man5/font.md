## Name

font - Bitmap Font File format (.font)

## Synopsis

Font files contain bitmap definitions of fonts (`Gfx::BitmapFont`).

## Description

Bitmap fonts can be either varying-width or fixed-width.
The first four bytes of font files contain the filemagic: `!Fnt` (0x21466e74).

In addition, `Gfx::BitmapFont` supports reading from and writing to font files (as well as reading directly from memory)
and the question mark '?' used as a fallback for unknown glyphs or emojis.

## Structure

The order is big-endian.

| Size     | Member name         |
| -------- | ------------------- |
| 4 bytes  | Filemagic           |
| 1 byte   | Glyph width         |
| 1 byte   | Glyph height        |
| 2 bytes  | Range mask size     |
| 1 byte   | Variable width flag |
| 1 byte   | Glyph spacing       |
| 1 byte   | Baseline            |
| 1 byte   | Mean line           |
| 1 byte   | Presentation size   |
| 2 bytes  | Weight              |
| 1 byte   | Slope               |
| 32 bytes | Name                |
| 32 bytes | Family              |

## See also

-   Format header definition in `Gfx::FontFileHeader` in [`Userland/Libraries/LibGfx/Font/BitmapFont.cpp`](../../../../../Userland/Libraries/LibGfx/Font/BitmapFont.cpp)
