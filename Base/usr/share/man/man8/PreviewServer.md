## Name

PreviewServer - File preview provider service

## Synopsis

```sh
$ PreviewServer
```

## Description

PreviewServer is a service that provides GUI applications with file previews. PreviewServer is responsible for choosing a service for rendering the preview and caching previews so that they may be accessed faster in the future. PreviewServer may keep both an in-memory cache as well as an on-disk cache (see [Files](#files)).

There may be several reasons why a preview for a file is not generated:

-   There is no preview provider for that file type.
-   The file was not recognized by the preview provider responsible for it.
-   No previews are generated for the preview cache directory.
-   No previews are generated for directories containing a `.nomedia` file. This allows the user to specify directories without media files where previews should not be generated, including for performance reasons.

## Files

PreviewServer uses the directory `.cache/preview` for storing preview caches. Deleting this directory has no impact on the ability of PreviewServer to operate; however it may cause slowdowns since previews need to be re-rendered.

Within `.cache/preview`, preview images are stored as QOI images of size 32×32. The base name is the hexadecimal SHA-512 hash of the file name and the 64-bit UNIX timestamp of the file's last modification time, in that order. SHA-512 ensures that even accidental hash collisions between distinct files are incredibly unlikely.
