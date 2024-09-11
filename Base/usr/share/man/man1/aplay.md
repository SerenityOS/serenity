## Name

aplay - play audio

## Synopsis

```**sh
$ aplay [--loop] [--sample-progress] <path>
```

## Description

This program plays an audio file specified in `path` through AudioServer.

## Options

-   `-l`, `--loop`: Loop playback
-   `-s`, `--sample-progress`: Switch to (old-style) sample playback progress. By default, playback is printed as played, remaining and total length, all in minutes and seconds.

## Arguments

-   `path`: Path to audio file

## Examples

```sh
$ aplay ~/sound.wav
$ aplay -l ~/music.flac
```
