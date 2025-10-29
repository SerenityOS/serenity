# `RAMFS` filesystem and its purposes

`RAMFS` is a RAM-backed filesystem. It is used to hold files and directories in the `/tmp` directory and
device nodes in the `/dev` directory.

## What are the `RAMFS` filesystem characteristics?

`RAMFS` is a pure RAM-backed filesystem, which means all files and directories
actually live in memory, each in its own `RAMFS` instance in the kernel.

The `RAMFS` in its current design is very conservative about allocating virtual memory ranges
for itself, and instead it uses the `AnonymousVMObject` object to hold physical pages containing
data for its inodes. When doing actual IO, the `RAMFS` code temporarily allocates a small virtual memory
`Memory::Region` to perform the task, which works quite well although it puts a strain on the virtual memory
mapping code. The current design also ensures that fabricated huge files can be easily created in the filesystem
with very small overhead until actual IO is performed.

### The `/tmp` directory and its purposes

Currently, the `/tmp` directory is the **place** for facilitating the inter-process
communication layer, with many Unix sockets nodes being present in the directory.

Many test suites in the project leverage `/tmp` for placing their test files
when trying to check the correctness of many system-related functionality.
Other programs rely on `/tmp` for placing their temporary files to properly function.

### Why does the `RAMFS` work well for the `/dev` directory?

To understand why `RAMFS` works reliably when mounted on `/dev`, we must understand
first what we did in the past and how `RAMFS` solves many of the issues with the previous design.

At first, we didn't have any special filesystem mounted in `/dev` as the image build
script generated all the required device nodes in `/dev`. This was quite sufficient in
the early days of the project, where hardware support was extremely limited and of course
hotplugging any kind of hardware was not even a consideration.

As the project grew larger and more hardware support was introduced, it became obvious
that this "solution" was not future-proof. For example, if one user has two SATA drives
connected to their computer, and another user has just one old IDE drive being used,
then how should we support both cases? The answer was that each user could simply invoke
the `mknod` utility to create device nodes. This solution meant that user interaction as well
as a deep understanding of kernel internals was required to achieve a proper setup.

When it became apparent that another solution was needed, the `DevFS` filesystem was
invented. The idea was plain simple - the `DevFS` is a read-only filesystem that only
lists all present char and block devices. Permissions were hardcoded at known value,
and modifying the filesystem (including adding subdirectories) was strictly prohibited.
This solution was efficient in the sense of ensuring minimal user interaction for using
device nodes in `/dev`. The shortcomings were strictly immutable filesystem layout and hardcoded
permissions. Also, the filesystem implementation was specific to `/dev`, because no other
mount in the system used this special filesystem, which meant it needed special test cases, etc.

The `DevFS` solution was short-lived, and was quickly replaced by the `DevTmpFS` solution.
That new shiny filesystem was again specific to `/dev`, but it solved many of the issues
`DevFS` suffered from - no more hardcoded permissions and now the design has flexible filesystem
layout in its mindset.
This was achieved by implementing from scratch a filesystem that resembles the `RAMFS`
filesystem, but was different in one major aspect - only device nodes and directories are allowed
to be in `/dev`. This strict requirement has been mandated to ensure the user doesn't
accidentally put unrelated files in `/dev`. When the `DevTmpFS` was invented, it clearly
needed userspace cooperation to create device nodes in `/dev`, so `SystemServer` was modified
to create those during boot. The process of how `SystemServer` does that is not discussed
in this document, but ultimately evolved to be flexible enough to work quite well.

Everything worked quite well, but there was still a prominent problem with `DevTmpFS` -
it was an entire filesystem solution just for `/dev` and nobody else used it.
Testing the filesystem was quite clunky and truthfully lacking from the beginning until its removal.
To solve this problem, it was decided to stop using it, and instead just use `RAMFS`.
To ensure the current behavior of disallowing regular files in `/dev`, a new mount flag called
`MS_NOREGULAR` was invented, so it could be mounted with it.
