## Name

PP - Pixel Paint application file format (.pp)

## Description

Pixel Paint files store the drawing data produced by the Pixel Paint application.

This is a rough overview of the contents of the files:

-   width
-   height
-   layers (optional)
    -   width
    -   height
    -   name
    -   locationx
    -   locationy
    -   opacity_percent
    -   visible
    -   selected
    -   bitmap
-   guides (optional)
    -   offset
    -   orientation

## See also

-   [`Userland/Applications/PixelPaint/Image.cpp`](../../../../../Userland/Applications/PixelPaint/Image.cpp)
