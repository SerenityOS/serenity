# DevTmpFS and its purposes

`DevTmpFS` is the current filesystem to hold device nodes in the /dev directory.

## What is DevTmpFS, what we used before and why do we need all of that

`DevTmpFS` is the next step after `DevFS`, which was the next step after we had 
nothing at all - just a build script that generates these device nodes for you,
statically.

### The build image script

At first, when the user compiled Serenity, the build image script mounted the
new formatted image, created the /dev directory, then did a bunch of `mknod`
commands on that directory to create many essential device nodes. These devices,
were actually written to the disk, so they were not stored in the RAM at all.
It was a static solution, and if someone wanted to utilize a second harddrive or
so, he had to ensure the build script includes `/dev/hdb` device node, and if
that harddrive had a partition on it, to also include `/dev/hdb1`.

This solution was good for its time, serving us for a long time, until it was
figured that this is not feasible nor the right way to expand in the future.

### DevFS - the first solution for automatic device nodes

`DevFS` was the first step into making the process of creating device nodes
automated. The basic idea is to mount a filesystem in /dev directory, and to let
it to handle device nodes for you. As a result you store all device nodes in
RAM, meaning the build image script doesn't need to create device nodes for you
anymore, which was a great thing at the moment. However, due to limitations in
the `DevFS` architecture - hardcoding names, hardcoding permissions for device
nodes and the lack of extensibility for future improvements in hotplug
capabilities in the Kernel, it was a short-lived solution and was deprecated for
the next solution - `DevTmpFS`.

As a side note, Linux also used `DevFS` in the past, but deprecated it for the 
same reason we did it in Serenity.

### DevTmpFS - the last solution solution so far 

`DevTmpFS` is really a `TmpFS` for the most part, with some restrictive rules. 
The implementation is not inheriting anything from `TmpFS`, for a couple of good
reasons:

1. `TmpFS` allows all sorts of good and bad (for `DevTmpFS`) files to be created
in any instance. We certainly don't want userland to create garbage files in a
`DevTmpFS` instance.
2. `TmpFSInode` is declared as final class. Although this can be changed, it 
will make things complicated unnecessarily. As an extension of the previous 
reason, we certainly don't want to make things more complicated and want to 
enforce some restrictions on what can live in a `DevTmpFS` instance.

## 2 DevTmpFS rules

There are 2 rules `DevTmpFS` has to follow:

1. `DevTmpFS` doesn't assume structure of filesystem, names, or paths at all. 
This means we don't hardcode anything in the implementation.
2. Only device nodes, symbolic links and directories are allowed to be created.
In contrast to `TmpFS`, we don't want unnecessary mess in this filesystem, and
trying to create anything from userspace that doesn't match this rule will fail.

Because nothing is created during the boot process (`DevFS` did that, `DevTmpFS`
doesn't), it is the userland's responsibility to generate the required device
nodes. How to create these device nodes is up to a userland program to decide,
the conventional method is to look into `/sys/dev/block` and `/sys/dev/char`
directories (which are created in the `SysFS` instance), and to look into major
and minor numbers (which are in the layout of the filename - "major:minor"),
then userspace can figure out based on this information which device nodes can 
be created in the /dev directory.
Note, however, that userspace can do so by using a statically-defined generate
script or a list that was prepared beforehand.

## What if you try to run the Shell (or other userland program) as init process?

If someone tries to boot into Shell then its their responsibility to ensure the
Shell creates the device nodes. This is not to be confused with the Serenity 
text mode boot option, which still uses `SystemServer` as the init process.
A user that desires to boot into Shell can also decide to statically generate
these device nodes beforehand in the disk image.

Still, that's a good question that we need to tackle with. The simple answer is
that its the responsibility of userspace now to generate the device nodes. In 
Linux, developers argued on whether to add hardcoded names and permissions back
into their `DevTmpFS` (they call it devtmpfs) implementation.
Although there are strong arguments for both sides, `DevTmpFS` in Serenity won't
add names and permissions back into it, unless there's a valid reason for it,
and by going into this pattern again, we will re-introduce unnecessary 
complexity for no strong reason for now.
