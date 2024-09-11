## Name

abench - benchmark audio decoders

## Synopsis

```**sh
$ abench [--sample-count samples] <path>
```

## Description

This program can be used to benchmark the performance of audio decoder plugins in LibAudio. It reports the raw decoding speed that is achieved on the given input file, without any overhead from resampling or actually playing the file. It is not only useful for benchmarking the decode speed of the file and/or profiling decoders, but also for checking conformance with (quirky) files.

While `abench` is running, it doesn't report anything to make measurements more accurate. After running, abench reports sample count, loader runtime, µs/sample, realtime speed and (for reference) realtime µs/Sample. "Realtime speed" refers to how much faster the loader is compared to playing the file, and "realtime µs/sample" then refers to the amount of time each sample normally takes up when played back. When realtime speed is over 100%, it means that the loader can load the file while it is playing at the same time.

## Options

-   `-s`, `--sample-count`: How many samples to load at maximum. This allows you to only benchmark some initial chunk of the file, which is useful when testing on quirky files that happen to be large.

## Arguments

-   `path`: Path to audio file. As usual, the file type is determined automatically.

## Examples

```sh
$ abench ~/sound.flac
$ abench -s 20000 ~/music.flac
```
