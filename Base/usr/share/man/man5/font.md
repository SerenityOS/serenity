## Name

font - Bitmap Font File format (.font)

## Synopsis

Font files contain bitmap definitions of fonts (Gfx::BitmapFont).

## Description

Bitmap fonts can be either varying-width or fixed-width.

There is a list of features:

- The first four bytes contain the filemagic: !Fnt (0x21466e74).
- Question mark '?' is used as a fallback for unknown glyph or emoji.
- Gfx::BitmapFont supports read from and write to the font files (as well as read directly from the memory).

## Structure

| size       | member name         |
|------------|---------------------|
| [4 bytes]  | filemagic           |
| [1 byte]   | glyph width         |
| [1 byte]   | glyph height        |
| [2 bytes]  | range mask size     |
| [1 byte]   | variable width flag |
| [1 byte]   | glyph spacing       |
| [1 byte]   | baseline            |
| [1 byte]   | mean line           |
| [1 byte]   | presentation size   |
| [2 bytes]  | weight              |
| [1 byte]   | slope               |
| [32 bytes] | name                |
| [32 bytes] | family              |

## See also

- Userland/Libraries/LibGfx/Font/BitmapFont.cpp (format header definition in Gfx::FontFileHeader)
