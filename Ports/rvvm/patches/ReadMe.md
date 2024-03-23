# Patches for rvvm on SerenityOS

## `0001-Disable-threaded-IO-on-Serenity.patch`

Disable threaded IO on Serenity

Due to sloppy scheduler/threading behavior on Serenity,
threaded IO is disabled in this port for now.
Otherwise U-Boot randomly fails to read data from NVMe,
or fails to initialize NVMe altogether, along with other IO
issues in guests - all due to threaded tasks being randomly
delayed for very long.

I am not an expert on how scheduler works in Serenity,
so I am unable to fix it yet.
This problem was also visible in previous v0.5 version of this port,
but back then I thought it's some kind of a temporary problem.
Couldn't reproduce this on any other host OS.

