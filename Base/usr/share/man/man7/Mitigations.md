## Name

Mitigations - Security mitigations implemented by SerenityOS

## Description

The SerenityOS developers have put substantial effort into
integrating various mitigation technologies into the system
in order to enhance its security. The goal of this document is
to collect and describe the mitigations in one centralized place.

## List of Mitigations

### SMEP (Supervisor Mode Execution Protection)

[Supervisor Mode Execution Protection](https://software.intel.com/security-software-guidance/best-practices/related-intel-security-features-technologies) is an Intel CPU feature which prevents execution
of userspace code with kernel privileges.

It was enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/8602fa5b49aa4e2b039764a14698f0baa3ad0532):
```
commit 8602fa5b49aa4e2b039764a14698f0baa3ad0532
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Wed Jan 1 01:56:58 2020 +0100

Kernel: Enable x86 SMEP (Supervisor Mode Execution Protection)
```

### SMAP (Supervisor Mode Access Prevention)

[Supervisor Mode Access Prevention](https://en.wikipedia.org/wiki/Supervisor_Mode_Access_Prevention)
complements SMEP by also guarding read/write access to
userspace memory while executing in kernel mode.

It was enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/9eef39d68a99c5e29099ae4eb4a56934b35eecde):

```
commit 9eef39d68a99c5e29099ae4eb4a56934b35eecde
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Sun Jan 5 18:00:15 2020 +0100

Kernel: Start implementing x86 SMAP support
```

### Pledge

[pledge](https://marc.info/?l=openbsd-tech&m=143725996614627&w=2) is a mitigation which originated from OpenBSD.
It allows a program to voluntarily restrict its access to system calls
and kernel facilities.

It was first added in the following [commit](https://github.com/SerenityOS/serenity/commit/41c504a33becea8aa9b437cd3c0dc312b2bf1fe9),
and the majority of programs were enlightened later:

```
commit 41c504a33becea8aa9b437cd3c0dc312b2bf1fe9
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Sat Jan 11 20:45:51 2020 +0100

Kernel: Add pledge() syscall :^)
```

### Unveil

[unveil](https://lwn.net/Articles/767137/) is a mitigation originating from OpenBSD.
It allows a program to voluntarily restrict its access to the filesystem.

It was first added in the following [commit](https://github.com/SerenityOS/serenity/commit/0569123ad7cb9c54df724c2bb85933ea3cf97134),
and the majority of programs were enlightened later:

```
commit 0569123ad7cb9c54df724c2bb85933ea3cf97134
Author: Andreas Kling <kling@serenityos.org>
Date:   Mon Jan 20 22:12:04 2020 +0100

Kernel: Add a basic implementation of unveil()
```

### Readonly atexit

[Readonly atexit](https://isopenbsdsecu.re/mitigations/atexit_hardening/) is a mitigation originating from OpenBSD.
Thanks to it, an attacker can no longer use the atexit region to escalate from arbitrary-write to code-execution.

It was first added in the following [commit](https://github.com/SerenityOS/serenity/commit/553361d83f7bc6499dc4821eff9b23a6549bd99c),
and was later [improved](https://github.com/SerenityOS/serenity/commit/fb003d71c2becf0b3ea148aad08642e5a7ea35bc)
to incur no additional cost during program initialization and finalization:

```
commit 553361d83f7bc6499dc4821eff9b23a6549bd99c
Author: Andreas Kling <kling@serenityos.org>
Date:   Sat Jan 30 10:34:41 2021 +0100

LibC: Protect the atexit() handler list when not writing to it

Remap the list of atexit handlers as read-only while we're not actively
writing to it. This prevents an attacker from using a memory write
primitive to gain code execution via the atexit list.

This is based on a technique used in OpenBSD. :^)
```

### Syscall call-from verification

[Syscall call-from verification](https://marc.info/?l=openbsd-tech&m=157488907117170&w=2) is
a mitigation which originated from OpenBSD.
In short the kernel checks that all syscalls originate
from the address of the system's libc. This makes attacks
on OpenBSD more difficult as they random-relink their libc
on boot, which makes finding syscall stubs in libc difficult
for attackers. On serenity it is mostly just an inconvenience,
as there currently is no libc random-relinking.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/823186031d9250217f9a51829d34a96b74113334):

```
commit 823186031d9250217f9a51829d34a96b74113334
Author Andreas Kling <kling@serenityos.org>
Date:  Tue Feb 2 19:56:11 2021 +0100

Kernel: Add a way to specify which memory regions can make syscalls
```

### Post-init read-only memory

[Post-init read-only memory](https://lwn.net/Articles/666550/) is
a mitigation which originated from the Linux Kernel.
It tracks data that is initialized during kernel boot and never
changed again. Post kernel initialization, the memory is marked
read-only to protect it from potentially being modified by exploits.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/d8013c60bb52756788e747183572067d6e3f204a)
and other kernel data structures were enlightened later:

```
commit d8013c60bb52756788e747183572067d6e3f204a
Author: Andreas Kling <kling@serenityos.org>
Date:   Sun Feb 14 17:35:07 2021 +0100

Kernel: Add mechanism to make some memory read-only after init finishes
```

### KUBSAN (Kernel Undefined Behavior Sanitizer)

UndefinedBehaviorSANitizer is a dynamic analysis tool, implemented in GCC,
which instruments generated code to flag undefined behavior at runtime.
It can find various issues, including integer overflows, out-of-bounds array
accesses, type corruption, and more.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/d44be968938ecf95023351a358c43c4957638d87):
```
commit d44be968938ecf95023351a358c43c4957638d87
Author: Andreas Kling <kling@serenityos.org>
Date:   Fri Feb 5 19:44:26 2021 +0100

Kernel: KUBSAN! (Kernel Undefined Behavior SANitizer) :^)
```

### Kernel unmap-after-init

Unmap-after-init allows the kernel to remove functions which contain potentially
dangerous [ROP gadgets](https://en.wikipedia.org/wiki/Return-oriented_programming)
from kernel memory after we've booted up and they are no longer needed. Notably the
`write_cr4(..)` function used to control processor features like the SMEP/SMAP bits
in the CR4 register, and the `write_cr0(..)` function used to control processor features
like write protection, etc.

With this mitigation it is now more difficult to craft a kernel exploit to do something
like disabling SMEP / SMAP.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/6136faa4ebf6a878606f33bc03c5e62de9d5e662):
```
commit 6136faa4ebf6a878606f33bc03c5e62de9d5e662
Author: Andreas Kling <kling@serenityos.org>
Date:   Fri Feb 19 18:21:54 2021 +0100

Kernel: Add .unmap_after_init section for code we don't need after init
```

### Relocation Read-Only (RELRO)

[RELRO](https://hockeyinjune.medium.com/relro-relocation-read-only-c8d0933faef3) is a mitigation
in the linker and loader that hardens the data sections of an ELF binary.

When enabled, it segregates function pointers resolved by the dynamic loader
into a separate section of the runtime executable memory, and allows the loader
to make that memory read-only before passing control to the main executable.

This prevents attackers from overwriting the [Global Offset Table (GOT)](https://en.wikipedia.org/wiki/Global_Offset_Table).

It was first enabled for executables in the following [commit](https://github.com/SerenityOS/serenity/commit/fa4c249425a65076ca04a3cb0c173d49472796fb):
```
commit fa4c249425a65076ca04a3cb0c173d49472796fb
Author: Andreas Kling <kling@serenityos.org>
Date:   Thu Feb 18 18:43:20 2021 +0100

LibELF+Userland: Enable RELRO for all userland executables :^)
```

Shared libraries were enabled in a follow-up [commit](https://github.com/SerenityOS/serenity/commit/713b3b36be4f659e58e253b6c830509898dbd2fa):
```
commit 713b3b36be4f659e58e253b6c830509898dbd2fa
Author: Andreas Kling <kling@serenityos.org>
Date:   Thu Feb 18 22:49:58 2021 +0100

DynamicLoader+Userland: Enable RELRO for shared libraries as well :^)
```

### -fstack-clash-protection

The GCC compiler option [`-fstack-clash-protection`](https://gcc.gnu.org/onlinedocs/gcc/Instrumentation-Options.html)
is a mitigation which helps prevent [stack clash](https://blog.qualys.com/vulnerabilities-research/2017/06/19/the-stack-clash)
style attacks by generating code that probes the stack in page-sized increments to ensure a fault is provoked.
This prevents attackers from using a large stack allocation to "jump over" the stack guard page into adjacent memory.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/7142562310e631156d1f64aff22f068ae2c48a5e):
```
commit 7142562310e631156d1f64aff22f068ae2c48a5e
Author: Andreas Kling <kling@serenityos.org>
Date:   Fri Feb 19 09:11:02 2021 +0100

Everywhere: Build with -fstack-clash-protection
```

### -fstack-protector / -fstack-protector-strong

The GCC compiler provides a few variants of the `-fstack-protector` option mitigation.
This family of flags enables [buffer overflow protection](https://en.wikipedia.org/wiki/Buffer_overflow_protection)
to mitigate [stack-smashing attacks](https://en.wikipedia.org/wiki/Stack_buffer_overflow).

The compiler implements the mitigation by storing a canary value randomized on program startup into the preamble of all
functions. Code is then generated to validate that stack canary on function return and crash if the value has been changed
(and hence a stack corruption has been detected.)

`-fstack-protector` was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/842716a0b5eceb8db31416cd643720c1037032b2):

```
commit 842716a0b5eceb8db31416cd643720c1037032b2
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Fri Dec 20 20:51:50 2019 +0100

Kernel+LibC: Build with basic -fstack-protector support
```

It was later re-enabled and refined to `-fstack-protector-strong` in the following commits:

```
commit fd08c93ef57f71360d74b035214c71d7f7bfc5b8
Author: Brian Gianforcaro <b.gianfo@gmail.com>
Date:   Sat Jan 2 04:27:35 2021 -0800

LibC: Randomize the stack check cookie value on initialization

commit 79328b2aba6192caf28f560881e56ff23fcb7294
Author: Brian Gianforcaro <b.gianfo@gmail.com>
Date:   Sat Jan 2 03:02:42 2021 -0800

Kernel: Enable -fstack-protector-strong (again)

commit 06da50afc71a5ab2bc63de54c66930a2dbe379cd
Author: Brian Gianforcaro <b.gianfo@gmail.com>
Date:   Fri Jan 1 15:27:42 2021 -0800

Build + LibC: Enable -fstack-protector-strong in user space
```
### Protected Kernel Process Data

The kernel applies a exploit mitigation technique where vulnerable data
related to the state of a process is separated out into it's own region
in memory which is always remmaped as read-only after it's initialized
or updated. This means that an attacker needs more than an arbitrary
kernel write primitive to be able to elevate a process to root for example.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/cbcf891040e9921ff628fdda668c9738f358a178):
```
commit cbcf891040e9921ff628fdda668c9738f358a178
Author: Andreas Kling <kling@serenityos.org>
Date:   Wed Mar 10 19:59:46 2021 +0100

Kernel: Move select Process members into protected memory
```

### -fzero-call-used-regs

GCC-11 added a new option `-fzero-call-used-regs` which causes the
compiler to zero function arguments before return of a function. The
goal being to reduce the possible attack surface by disarming ROP
gadgets that might be potentially useful to attackers, and reducing
the risk of information leaks via stale register data.

It was first enabled when compiling the Kernel in the following [commit](https://github.com/SerenityOS/serenity/commit/204d5ff8f86547a8b100cf26a958aaabf49211f2):

```
commit 204d5ff8f86547a8b100cf26a958aaabf49211f2
Author: Brian Gianforcaro <bgianf@serenityos.org>
Date:   Fri Jul 23 00:42:54 2021 -0700

Kernel: Reduce useful ROP gadgets by zeroing used function registers
```

### Linking with "separate-code"

The linker is passed the `separate-code` option, so it won't combine read-only data
and executable code. This reduces the total amount of executable pages in the system.

It was first enabled in the following [commit](https://github.com/SerenityOS/serenity/commit/fac0bbe739154abb416526bdc983487c05ba0c81):

```
commit fac0bbe739154abb416526bdc983487c05ba0c81
Author: Andreas Kling <kling@serenityos.org>
Date:   Tue Aug 31 16:08:11 2021 +0200

Build: Pass "-z separate-code" to linker
```

## See also

* [`unveil`(2)](../man2/unveil.md)
* [`pledge`(2)](../man2/pledge.md)
