## Name

![Icon](/res/icons/16x16/filetype-image.png) Image Viewer - SerenityOS image viewer

[Open](file:///bin/ImageViewer)

## Synopsis

```**sh
$ ImageViewer [file] [-f] [-h Height] [-w Width] [-b bitmap_format]
```

## Description

ImageViewer is an image viewing application for SerenityOS.

For user convenience, basic image manipulation facilities exist like image rotation clockwise or counter-clockwise, image flip horizontal or vertical, zoom in, zoom out, zoom reset, fullscreen view and fit image to view. 

File manipulation has no effect on the image. Flip or rotate actions are not saved or committed, it is simply ignored when the application is closed. 

ImageViewer is even smart enough to detect other images and display them when clicking on the navigation buttons or when using direction arrows. ImageViewer can even set the currently loaded image as a Desktop Wallpaper.

## Options

* `-f`, `--fallback-binary`: Try to decode a file as an image of a known format, otherwise fallback to use it as a raw binary bitmap data
* `-w Width`, `--width Width`: Force width when displaying an image
* `-h Height`, `--height Height`: Force height when displaying an image
* `-b [bgrx, bgra, rgbx, rgba]`, `--bitmap-format [bgrx, bgra, rgbx, rgba]`: Force bitmap format when decoding as a raw binary bitmap file

## Arguments

* `file`: The image file to be displayed.

## Examples

```sh
# Display the known Buggie picture
$ ImageViewer /res/graphics/buggie.png
# Display a raw bitmap binary file, with width of 220 pixels and height of 100, encoded in RGBA8888 format.
$ ImageViewer -f -w 220 -h 100 -b rgba example.bin
```

