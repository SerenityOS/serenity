## Name

font - Bitmap Font File format

## Synopsis

The .font file format stores bitmap fonts in SerenityOS's own binary format.

## Description

These files contain bitmap definitions of fonts, either varying-width or fixed-width.

The first four bytes contain the filemagic: `!Fnt` (0x21466e74).

## See also

- Format header definition in `Gfx::FontFileHeader` in [`Userland/Libraries/LibGfx/Font/BitmapFont.cpp`](../../../../../Userland/Libraries/LibGfx/Font/BitmapFont.cpp).
