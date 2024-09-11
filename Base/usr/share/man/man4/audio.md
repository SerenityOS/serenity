## Name

audio - system audio devices

## Description

`/dev/audio` is the directory for the system audio devices. Currently, there are only output devices, so every device file in the directory is an output channel. These channels are numbered, with `/dev/audio/0` being the first channel of the first device. To get the audio device to play audio, PCM samples need to be written to it as a series of "frames" (in MPEG terminology) or multi-channel samples with the following format:

| Byte   |      0-1      |      2-3      |
| ------ | :-----------: | :-----------: |
| Format | 16-bit signed | 16-bit signed |
| Data   |  Left sample  | Right sample  |

The sample rate of the samples is determined by the audio device's current sample rate, which may be accessed by an [ioctl](help://man/2/ioctl).

Note that for convenience, the audio device may not block the call to `write` and return before all the samples were actually transferred to the hardware and/or played by the hardware. For this reason, users need to be aware that the audio device driver's internal buffer may become full and calls to `write` may return `ENOSPC`.

## Available `ioctl`s

-   `SOUNDCARD_IOCTL_GET_SAMPLE_RATE`: Passes the current device sample rate (in samples per second) into a provided `u16*` (16-bit unsigned integer pointer).
-   `SOUNDCARD_IOCTL_SET_SAMPLE_RATE`: Sets the sample rate of the underlying hardware from a provided 16-bit unsigned integer. Note that not all sound cards support all sample rate and the actually achieved sample rate should be checked with the GET_SAMPLE_RATE ioctl.

## See also

-   [Audio-subsystem](help://man/7/Audio-subsystem)
