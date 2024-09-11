## Name

asctl - Send control signals to the audio server and hardware

## Synopsis

```**sh
$ asctl [--human-readable] <command> [args...]
```

## Description

This program is used to send control signals to the AudioServer and the sound hardware. This allows changing audio server variables like volume and mute state, as well as querying the state of these variables.

## Options

-   `-h`, `--human-readable`: Print human-readable output. If this option is not given, the output of `get` will be machine-readable and only consist of one line.

## Arguments

-   `command`: The command to execute, either `get` or `set`.
-   `args`: The arguments to the command.

There are two commands available: `get` reports the state of audio variables, and `set` changes these variables.

`get` expects a list of variables to report back, and it will report them in the order given. The exact format of the report depends on the `--human-readable` flag. If no variables are given, `get` will report all available variables, in the order that they are listed below.

`set` expects one or more variables followed by a value to set them to, and will set the variables to the given values. A variable can be given multiple times and the last specified value will remain with the audio server.

The available variables are:

-   `(v)olume`: Audio server volume, in percent. Integer value.
-   `(m)ute`: Mute state. Boolean value, may be set with `0`, `false` or `1`, `true`.
-   `sample(r)ate`: Sample rate of the sound card. Integer value.

Both commands and arguments can be abbreviated: Commands by their first letter, arguments by the letter in parenthesis.

## Examples

```**sh
Get the current volume (machine format)
$ asctl get volume
100

Get all variables
$ asctl -h get
Volume: 100
Muted: No
Sample rate: 48000 Hz

Set the volume to 100%
$ asctl set volume 100

Mute all audio
$ asctl set mute true

Unmute all audio, set volume to 80%
$ asctl s m 0 v 80

Set sample rate
$ asctl s samplerate 48000
```
