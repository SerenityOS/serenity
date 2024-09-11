# ProcFS Indexing

## Is a ProcFS index deterministic value?

Short answer - yes. Long answer - because of the design pattern that was chosen,
each `InodeIndex` actually represent a known object, so it is guaranteed to be
the same always for global ProcFS objects. For process ID directories, once that
process has been killed, its primary segment value is no longer valid and hence
all sub-segments of it are not relevant anymore, but if the process is still alive,
it is guaranteed that accessing the same `InodeIndex` in regard to an object tied to
a process directory will provide the expected object.

## The goal - zero allocations when creating new process

The main goal is to have zero allocations happening in ProcFS when a new process is created.
The old ProcFS design followed that principle, but was quite hard to edit and to extend with new
functionality.
The current ProcFS design doesn't follow that principle, but is easier to edit and to extend.
A compromise is needed to ensure we get the advantages from both designs while minimizing the
effects of the disadvantages of each design.

## The segmented index

### The layout of the segmented index

Since it was decided that heap allocations for ProcFS are _mostly_ bad, the new
design layout tries to achieve most of the principle of "Don't allocate anything
until actually needed". For that to happen, `InodeIndex` (u64 value) is split
into 3 Segments:

-   The primary segment: value 0 is reserved for all non-PID inodes in the procfs.
    All values from 1 to 0xFFFFFFF are valid PID indices, which represents all PIDs from 0 to 0xFFFFFFE

-   The Sub-directory segment: value 0 is reserved for parent PID directory. All other values are
    available for usage of sub-directories in the PID directory.

-   The property segment: value 0 is reserved for parent PID directory. All other values are
    available for usage of components in the PID directory or in sub-directories of the PID directory.

So, the final layout of the 64 bit index is:

```
| Primary Segment (28 bits) | Sub-directory (16 bits) | Component (20 bits) |
```

Example: To find a Thread 0 stack, for PID 1, the following encoding is applied:

```
hex(2 << 16 | 2 << (16 + 28)) == 0x200000020000
```

### Two rules for indexing

We don't want to allocate anything when a process is created, but we still want
to allocate global objects, so it's somewhat a compromise between two conflicting targets.
To do that we need to ensure that:

1. If the primary segment value equals to 0, then the sub-directory and property segmentation
   is not applied, but a sequential indexing is determined instead. This is needed so ProcFS can still
   use global components that were pre-allocated beforehand. This means that there might be up to
   68719476735 global components (including global sub-directories objects) in the ProcFS.
   Otherwise, for every primary segment value > 0, then the sub-directory and property segmentation
   is applied. This means that there might be up to 65534 sub-directories in a PID directory, and
   up to 1048575 (1048574 for PID directory) properties (objects) in each sub-directory.

2. If the primary segment value equals to 0, then value 0 in both artificial sub-directory
   and property segments represents the root ProcFS folder.
   Otherwise, for every primary segment value > 0, value 0 in both sub-directory and
   property segments are reserved to represent the root PID directory.
   Please note that if the sub-directory segment > 0, and property segment = 0 is a valid
   index, and represents a valid property object in that sub-directory.
