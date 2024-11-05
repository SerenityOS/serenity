## Name

![Icon](/res/icons/16x16/app-3d-file-viewer.png) 3D File Viewer

[Open](file:///bin/3DFileViewer)

## Synopsis

```**sh
$ 3DFileViewer [file]
```

## Description

`3D File Viewer` is an application for viewing 3D models.

It currently supports opening the [Wavefront OBJ file format](https://en.wikipedia.org/wiki/Wavefront_.obj_file) (`.obj`).

## Features

By default, 3D File Viewer opens with a rotating [Utah teapot](https://en.wikipedia.org/wiki/Utah_teapot), a standard 3D test model. Open files via _File → Open_ (`Ctrl+O`) or dragging and dropping files into the application. Sample files can be found in `Documents/3D Models`.

Orbit around the model by grabbing it with the cursor, and zoom in and out with the mouse wheel.

### View Options

-   View in Fullscreen mode by pressing `F11` and press `Esc` to return to windowed mode.
-   **Rotation Axis** - Enable or disable rotation:
    -   **X** rotates upwards.
    -   **Y** rotates right.
    -   **Z** rotates clockwise.
-   **Rotation Speed** - Set to slow, normal, fast or disable.
-   **Show Frame Rate** - Display FPS (frames per second) and frame time values (indicating how long it took to render a single frame) in the top-right corner.

### Texture Options

To load a texture, ensure there's a [Bitmap image file](https://en.wikipedia.org/wiki/BMP_file_format) (`.bmp`) with the same name as the OBJ file in the same folder and 3D File Viewer will attempt to load it automatically.

-   **Enable Texture** - On by default, disable for a blank (gray) model.
-   **Wrap S or T** - Controls how a texture is applied horizontally (S) or vertically (T):
    -   **Repeat** - Tiles the texture along the axis.
    -   **Mirrored** - Tiles and mirrors the texture along the axis.
    -   **Clamp** - Stretches or clamps the texture edges instead of repeating it.
-   **Scale** - Adjusts texture size (e.g. `0.5` reduces by half, `2x` doubles the size).
-   **Mag Filter** - Determines the appearance of textures when enlarged beyond their original resolution.
    -   **Nearest** - _Nearest Neighbor_ means no scaling is applied to the texture, resulting in a pixellated look.
    -   **Linear** - _Linear Interpolation_ scales the image by estimating pixel values, resulting in a smoother image.

Currently, the flashing color lights can't be disabled.

Settings persist whilst the application is open with different files, but do not persist between sessions.

## Arguments

-   `file`: The 3D file to be viewed.

## Example

```**sh
$ 3DFileViewer Documents/3D\ Models/ladyball.obj
```
