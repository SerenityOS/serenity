## Name

aplay - play a sound

## Synopsis

```**sh
$ aplay [--loop] <path>
```

## Description

This program plays a sound specified in `path` through AudioServer.

## Options

* `-l`, `--loop`: Loop playback

## Arguments

* `path`: Path to audio file

## Examples

```sh
$ aplay ~/sound.wav
$ aplay -l ~/music.wav
```
