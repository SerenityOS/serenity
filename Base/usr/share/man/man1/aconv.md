## Name

aconv - convert between audio formats

## Synopsis

```**sh
$ aconv -i input [--input-audio-codec input-codec] [--audio-codec output-codec] [--audio-format sample-format] -o output
```

## Description

`aconv` converts between different audio formats, or more precisely, between containers and codecs. `aconv` can both read from and write to the standard input and output streams. Depending on the audio format used, `aconv` may or may not act as a streaming converter; that is, `aconv` may require to read until EOF of the input file to start writing to the output. More details can be found with the encoders and decoders that `aconv` supports.

The input file option is always required. `aconv` will try to guess which container and codec the input has, unless the `--input-audio-codec` option is provided. Note that guessing the codec is not possible for raw formats such as raw PCM streams, therefore such formats always require specifying the input codec option.

The output file may be omitted, in which case `aconv` does not write any data. If an output codec is provided via `--audio-codec`, `aconv` will internally still perform the conversion without writing data. If `--audio-codec` is not provided, `aconv` will decode the input and not do anything else. It is recommended that [`abench`](help://man/1/abench) be used for audio input testing purposes.

`aconv` will try to guess the output container and codec based on the file name specified. The codec can be overwritten with the aforementioned `--audio-codec` option; this is mandatory for the standard output stream where the container and codec cannot be guessed.

By specifying `--audio-format`, `aconv` will use a different sample format for the output than what the input file provides. The sample format is the format of the PCM stream that is encoded with the codec, and it specifies multiple parameters such as bit depth, data type, and endiannness. The supported sample formats depend on the codec, but they have common names shared across codecs.

### Supported Codecs and Containers

Note that `aconv` currently only supports codecs which have their own bespoke container. Therefore, the distinction does not currently matter. The names given below are the only recognized names for this codec for the command line options `--audio-codec` and `--input-audio-codec`. Some codecs can only be decoded or both encoded and decoded.

-   `mp3` (decode): MPEG Layer III audio codec and container.
-   `wav` (decode, encode): RIFF WAVE audio codec and container. Supports sample formats `u8` and `s16le` for writing.
-   `flac` (decode, encode): Free Lossless Audio Codec and container. Supports all integer sample formats for writing.
-   `qoa` (decode): Quite Okay Audio codec and container.

### Supported Sample Formats

-   `u8`: Unsigned 8-bit integer
-   `s16le`: Signed 16-bit integer, little endian
-   `s24le`: Signed 24-bit integer, little endian
-   `s32le`: Signed 32-bit integer, little endian
-   `f32le`: 32-bit IEEE 754 floating-point number (normalized to the range [-1, 1]), little endian
-   `f64le`: 64-bit IEEE 754 floating-point number (normalized to the range [-1, 1]), little endian

## Options

The option format follows this general pattern: `--input_or_output-stream-parameter`, where `input_or_output` is either `input` when concerning the input stream, or omitted for the output stream (since this is the more common use case), `stream` currently is always `audio`, and `parameter` is the parameter of the stream that is changed.

-   `-i`, `--input`: The input file to convert from. Use `-` to read from standard input.
-   `-o`, `--output`: The output file to write to. Use `-` to write to standard output.
-   `--input-audio-codec`: Overwrite the used codec and/or sample format of the input file.
-   `--audio-codec`: The codec to use for the output file.
-   `--audio-format`: The sample format to use for the output file.

## Examples

```sh
# Decode a FLAC file to WAV
$ aconv -i ~/sound.flac -o ~/sound.wav

# Decode an MP3 file stored in a metadata block of a FLAC file to WAV
$ aconv -i ~/has-mp3-contents.flac --input-audio-codec mp3 -o ~/weird.wav

# Recode WAV to 8-bit and output it to stdout
$ aconv -i ~/music.wav --audio-format u8 -o -
```

## See Also

-   [`abench`(1)](help://man/1/abench) to test audio decoders and measure their performance
-   [`aplay`(1)](help://man/1/aplay) to play audio files from the command line
