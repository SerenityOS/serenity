# Kernel Development Patterns & Guidelines

This document intends to guide the immediate and newcomer kernel developer when creating,
modifying and removing Kernel code.
Please read all of this document if you intend to send pull requests, as well as the [general contributing guidelines](../../CONTRIBUTING.md)
and [patterns](../Patterns.md) for the entire codebase.

This document was composed as a result of ideas, experience and a general vision of what
the Kernel could become in the prosperous future of the project.

## Out of memory handling

Maybe one of the most important issues we have to solve in kernel code is when OOM (Out of memory)
condition occurs - simply put, a new allocation request has failed due to various reasons -
the allocation request was too much "greedy" and couldn't be satisfied, or simply we can't allocate more physical RAM pages
to whoever that requested them.

**The proper solution to this is to always use the `TRY()` semantics together with
appropriate `adopt_*` function (either for `OwnPtr` or `RefPtr`).**

```cpp
#include <AK/Try.h>
#include <AK/OwnPtr.h>

...

auto new_object = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Object(...)));
```

In case of failure, the above code will simply propagate `ENOMEM` code to the caller, up to the syscall entry code,
so the userland program could know about the situation and act accordingly.

An exception to this is when there's simply no way to propagate the error code to the userland program.
Maybe it's a `ATAPort` (in the IDE ATA code) that asynchronously tries to handle reading data from the harddrive,
but because of the async operation, we can't send the `errno` code back to userland, so what we do is
to ensure that internal functions still use the `ErrorOr<>` return type, and in main calling function, we use
other meaningful infrastructure utilities in the Kernel to indicate that the operation failed.

## We don't break userspace - the SerenityOS version

We don't break userspace. However, in contrast to the Linux vision on this statement,
we don't care about ABI/API breakage between the userland and the kernel. **What we do care
about is a possible incident when a Kernel change does introduce a misbehave in userland, and Userland was not
appropriately considered to ensure this does not happen.**

Many internal changes in the Kernel don't affect userland - for example, a new shiny driver
for super-fast storage devices, is not something that will likely break userland, because the
proper abstractions have already put in place, so the userland simply does not care about
the specifics about each `StorageDevice` in the kernel, as long as it properly implements the
known interfaces.

However, some kernel changes, mainly ABI/API changes between userland and the kernel, in the
syscall handling layer, will break userland unless it's properly handled beforehand.
The proper solution in git terms is to ensure that both "offending" kernel changes and the appropriate
userland changes to accommodate the kernel changes are in the same commit, so we still keep the rule that
each git commit is bisectable by itself.

**It's expected that changes to the Kernel will be tested with userland utilities to ensure the changes
are not creating any misbehaves in the userland functionality.**

Even more stricter than what has been said above - we don't remove functionality unless it's absolutely
clear that nobody uses that functionality. Even when it's absolutely clear that nobody uses some kind
of kernel functionality, it could still be useful to think about how to make it more available and usable
to the SerenityOS project community.
Again, such removal should happen according to what has been mentioned in terms of git handling.

## Each kernel feature should be backed by a userland usecase

In contrast to the previous guideline, this guideline is clearly about the healthy growth of Kernel -
we don't bloat the Kernel for things we don't need. For example, in the early days
of the project, there was a floppy driver in the Kernel and it got removed because
nobody used it. Similarly, when an Intel AC97 soundcard driver was introduced, the SB16
soundcard driver was removed shortly afterwards. **We simply don't have interest in supporting
hardware that nobody will use, or a kernel feature that doesn't make sense to most people.**

## Proper locking

The [AHCI locking document](AHCILocking.md) describes our locking patterns thoroughly.
Still, it's very important to understand we do care about SMP (Symmetric Multiprocessing),
so proper locking is one of the top priorities in the kernel development mindset.

**The general rule is that we should not acquire a `Mutex` after taking a `Spinlock`.
Taking a `Spinlock` after another is generally considered fine, as long as they are always
taken in the same order, to prevent deadlocks.**

To ensure we do this properly, the `MutexProtected<>` and `SpinlockProtected<>` C++ containers
have been introduced in the kernel to ensure that locking is done on particular shared data objects,
so it's preferable to use these containers instead of a "random" spinlock as class member.

## Proper, clean and meaningful syscall userland interfaces

As at the time of writing this document, the syscall table is generally quite stable.
This happens to be that way because the syscalls are well-defined, backed by good-known POSIX interfaces.
**Suggestions/patches to add syscalls should be examined strictly, because generally-speaking it's the "last resort"
we should choose from other Unix interfaces that are available to us.**

Because there's no definitive "yes" or "no" for all cases, expect that a discussion will be taking
place in your pull request, in case that you do introduce a new syscall in the Kernel.

For example, say that one wants to add a new driver for the Storage subsystem, then
we already have the proper abstractions in place, so the new specific `StorageDevice` will be registered
as like any other `StorageDevice`, therefore it will be exposed in the `/dev` directory and regular
`write`, `open`, `read`, `ioctl` syscalls will be usable immediately.
Therefore, there's no need for a special syscall to handle the new hardware, because
the already-existing syscalls are sufficient.

**We should also refrain from architecture-specific syscalls as much as possible. Linux had them
in the past and many of them were removed eventually.**

## Security measures

We, as the SerenityOS project, take seriously the concept of security information.
Many security mitigations have been implemented in the Kernel, and are documented in a
[different file](../../Base/usr/share/man/man7/Mitigations.md).
As kernel developers, we should be even more stricter on the security measures being
taken than the rest of system.
One of the core guidelines in that aspect **is to never undermine any security measure
that was implemented, at the very least.**

It's also very nice and generous if one decides to improve on a security measure,
as long as it doesn't hurt other security measures.

We also consider performance metrics, so a tradeoff between two mostly-contradictive metrics
is to be discussed when an issue arises.

## No hardcoded userspace paths

To ensure the kernel stays flexible to future changes, we should not put hardcoded
paths or assume where filesystem items (nor where filesystems are mounted) reside on - we should
always let userspace to inform the kernel about paths and assume nothing else.
Even when it's obvious some file will always be located in a certain path, it is considered
a violation of an abstraction layer to hardcode it in the kernel code, because we put an hard effort
to keep the abstractions we have intact and clean.

There's one exception to this rule - the kernel will use a `dbgln` statement to
warn the user in case that the dynamic loader is not the usual binary we use.
To generalize the exception a bit more - debug messages (being used sparingly) with
assumption of paths could be OK, as long as they never have any functional implication
on the user.

## Documentation

As with any documentation, it's always good to see more of it, either with a new manual page,
or a kernel concept being described in the `Documentation/Kernel` repository directory so other
developers can understand it.
There's no well-defined template to use when writing a documentation, but it is expected
at the very least to have an opening paragraph about the topic so others can understand
what the document is about.
