# Patches for VVVVVV on SerenityOS

## `0001-Disable-SDL-renderer-HW-acceleration.patch`

Disable SDL renderer HW acceleration


## `0002-Correctly-calculate-FAudio-block-alignment.patch`

Correctly calculate FAudio block alignment

This fixes music playback on FAudio 26.01.

FAudio 26.01 added bounds checks to FAudioSourceVoice_SubmitSourceBuffer
in 033498ab08f5a1349ed5723a47aaa87163654be6.

The calculation of nBlockAlign is currently incorrect. nBlockAlign is
set to the block size in bits, not in bytes, causing this new bounds
check to trip.

Similarly, the wav_length for sound effects is in bytes.

## `0003-Remove-git-commit-info.patch`

Remove git commit info

We build the port from a tarball, so it will incorrectly use
SerenityOS's git commit hash.

