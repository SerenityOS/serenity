## Name

![Icon](/res/icons/16x16/app-image-viewer.png) Image Viewer - SerenityOS image viewer

[Open](file:///bin/ImageViewer)

## Synopsis

```**sh
$ ImageViewer [file]
```

## Description

ImageViewer is an image viewing application for SerenityOS.

For user convenience, basic image manipulation facilities exist like image rotation clockwise or counter-clockwise, image flip horizontal or vertical, zoom in, zoom out, zoom reset, fullscreen view and fit image to view.

File manipulation has no effect on the image. Flip or rotate actions are not saved or committed, it is simply ignored when the application is closed.

ImageViewer is even smart enough to detect other images and display them when clicking on the navigation buttons or when using direction arrows. ImageViewer can even set the currently loaded image as a Desktop Wallpaper.

## Arguments

-   `file`: The image file to be displayed.

## Examples

```sh
$ ImageViewer /res/graphics/buggie.png
```
