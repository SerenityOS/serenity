## Name

Overview of the SerenityOS audio subsystem, including a brief description of [`/dev/audio`](help://man/4/audio), the AudioServer and their interfaces. 

## Description

(Note that familiarity with the basics of digitized audio, pulse code modulation (PCM), sample rate and bit depth is assumed.)

SerenityOS structures audio into three groups of responsibilities: Audio drivers that talk to hardware and expose the `/dev/audio` device; the AudioServer that is responsible for talking to userland audio clients, mixing and processing audio, and controlling the hardware via the driver; the audio libraries LibAudio and LibDSP that facilitate easier handling of audio data for userland applications.

### AudioServer

AudioServer is responsible for handling userland audio clients and talking to the hardware. For this reason, no userland application should ever need to write to `/dev/audio` directly, except for special cases in which AudioServer is not present.

As with all system servers, AudioServer provides an IPC interface on `/tmp/portal/audio`. For specifics on how to talk to AudioServer, the IPC interface specifications are the best source of information. Audio clients send audio buffers with the standard audio format (see [audio](help://man/4/audio)) to the server. They can then query the state of these buffers, pause buffer playback or clear the playing buffers. For controlling mixer functionality, clients have the ability to obtain and change their own volume, or the main volume and mute state.

In reverse, AudioServer has "event" calls that the client receives. These are: A client buffer finished playing (useful for queuing the next buffer), various mixer states changed (main volume, main mute, client volume).

### Libraries

There are two complementary audio libraries.

#### LibAudio

LibAudio is the baseline audio library that provides common audio abstractions, such as audio buffers and samples. Additionally, an important feature of LibAudio are the Loaders and Writers. The Loader class provides a multitude of audio formats (currently: WAV and FLAC), can auto-detect the format of a file or stream and abstracts away the low-level complications of parsing and reading these formats. The various writer classes (currently: WAV) provide an abstraction over exporting audio in specific formats to disk.

#### LibDSP

LibDSP is the digital signal processing library. It provides structures for audio editing programs, such as tracks and clips, while both dealing with MIDI data and sample data. More important is the Processor system, which allows synthesizers, samplers, sequencers, effects, etc. to be written with a common interface and be combined into chains for unlimited DSP (and musical) potential. The ProcessorParameters provide an interface for changing processor parameters programmatically or through a UI.

LibDSP was started to support development efforts in Piano, but it is intended as a general-purpose audio processing library, building on the groundwork from LibAudio. Therefore, users of LibDSP must be familiar with LibAudio classes and concepts, as they are used extensively in LibDSP.

### Applications

This is a non-exhaustive list of applications that use audio. Most of these follow the good practices laid out in this manual page and may serve as a template for new audio applications.

* **Piano** is a sequencer/tracker and synthesizer.
* **aplay** is a command line audio file playback utility.
* **SoundPlayer** is a UI audio file player with extra features such as playlist support and audio visualizations.
* [**asctl**](help://man/1/asctl) is a command line audio server control utility.
* **Applets/Audio** (AudioApplet) is a taskbar applet for setting audio parameters through a UI.

### Volume

Audio volume is more complicated than just multiplying a (digital or analog) audio signal with a percentage volume value. As the human hearing is logarithmic, volume changes also need to be logarithmic. An excellent article on the topic can be found [here](https://www.dr-lex.be/info-stuff/volumecontrols.html).

For the SerenityOS audio system, the following applies: Userland applications and libraries that do their own volume changes need to be aware of the nature of volume. LibAudio provides utility functions for correctly handling volume, so these are to be used whenever applicable. For AudioServer, main and per-client volume is already handled correctly; to the outside, volume is linear between 0 and 1.

For example: A program may set its client volume to 0.5 and the audio will be perceived as half as loud by a human. However, if the program wishes to change the volume beforehand, it needs to use logarithmic scaling, for example with LibAudio's built-in functionality.

### Sample rate

SerenityOS's audio system has no fixed sample rate. The sample rate is ultimately determined by the sound hardware and all parts of the system need to be aware of it. Audio samples passed to AudioServer are assumed to be at the system sample rate, meaning that audio applications need to query this sample rate and resample their audio data accordingly.

Although the sample rate can change at any time, it is considered a rarely-changing value and most applications don't handle sample rate changes during operation. Therefore, when the system sample rate is changed, audio applications may need to be re-opened.

## Files

* [/dev/audio](help://man/4/audio)
* AudioApplet and AudioServer have settings which are managed by ConfigServer.
* `/tmp/portal/audio`: AudioServer's IPC socket

## See also

* [asctl](help://man/1/asctl)
* [aplay](help://man/1/aplay)
