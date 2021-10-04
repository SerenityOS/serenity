## Name

audio - system audio device

## Description

The `/dev/audio` character device file exposes the audio output device of the system. As of now, this is an output-only device and reading it has no effect. To get the audio device to play audio, PCM samples need to be written to `/dev/audio` as a series of "frames" (in MPEG terminology) or multi-channel samples with the following format:

| Byte | 0-1 | 2-3 |
|--|:--:|:--:|
| Format | 16-bit signed | 16-bit signed |
| Data | Left sample | Right sample |

The sample rate of the samples is determined by the audio device's current sample rate, which may be accessed by an [ioctl](../man2/ioctl.md).

Note that for convenience, the audio device may not block the call to `write` and return before all the samples were actually transferred to the hardware and/or played by the hardware. For this reason, users need to be aware that the audio device driver's internal buffer may become full and calls to `write` may return `ENOSPC`.

## Available `ioctl`s

* `SOUNDCARD_IOCTL_GET_SAMPLE_RATE`: Passes the current device sample rate (in samples per second) into a provided `u16*` (16-bit unsigned integer pointer).
* `SOUNDCARD_IOCTL_SET_SAMPLE_RATE`: Sets the sample rate of the underlying hardware from a provided 16-bit unsigned integer. Note that not all sound cards support all sample rate and the actually achieved sample rate should be checked with the GET_SAMPLE_RATE ioctl.
