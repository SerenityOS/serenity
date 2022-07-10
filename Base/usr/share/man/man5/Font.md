## Name

Font - system fonts (.font)

## Description

Font files contain bitmap definitions of fonts (Gfx::BitmapFont).

Bitmap fonts can be either varying-width or fixed-width.

Question mark '?' is used as a fallback for unknown glyph or emoji.

Additionally, Gfx::BitmapFont supports read and write to the font files (as well as read directly from the memory).

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
