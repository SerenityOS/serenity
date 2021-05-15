## Name

avol - Change system sound volume

## Synopsis

```**sh
$ avol [-m] [-M] [volume]
```

## Description

This program is used to control the AudioServer volume and muted state.

## Options

* `-m`: Mute all audio.
* `-M`: Unmute all audio.

## Examples

```sh
# Set the volume to 100
$ avol 100
Volume: 100

# Get the current volume
$ avol
Volume: 80

# Mute all audio
$ avol -m

# Unmute all audio
$ avol -M
```
