# Patches for FFmpeg on SerenityOS

## `errno-is-not-valid-during-cpp.patch`

FFmpeg performs a preprocessor-time test on errno values. This test does
not work on SerenityOS because our errno values are described by an
enum, and therefore are only available at compile time.

The patch removes the preprocessor-time test.
