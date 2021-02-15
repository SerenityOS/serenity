## Name

Mitigations - Security mitigations implemented by SerenityOS.

## Description

The SerenityOS developers have put substantial effort into
integrating various mitigation technologies into the system
in order to enhance its security. The goal of this document is
to collect and describe the mitigations in one centralized place.

## List of Mitigations

### SMEP (Supervisor Mode Execution Protection)

[Supervisor Mode Execution Protection](https://software.intel.com/security-software-guidance/best-practices/related-intel-security-features-technologies) is a feature
of Intel CPUs which allows the kernel to instruct the CPU
to disable execution of code residing in user space.

It was enabled in the following commit:
```
commit 8602fa5b49aa4e2b039764a14698f0baa3ad0532
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Wed Jan 1 01:56:58 2020 +0100

Kernel: Enable x86 SMEP (Supervisor Mode Execution Protection)

```

### SMAP (Supervisor Mode Access Prevention)

[Supervisor Mode Access Prevention](https://en.wikipedia.org/wiki/Supervisor_Mode_Access_Prevention) compliments
SMEP, it allows a kernel to set user-space memory mappings
that will cause a trap when accessing user-space memory.

It was enabled in the following commit:

```
commit 9eef39d68a99c5e29099ae4eb4a56934b35eecde
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Sun Jan 5 18:00:15 2020 +0100

Kernel: Start implementing x86 SMAP support
```

### Pledge

[pledge](https://marc.info/?l=openbsd-tech&m=143725996614627&w=2) is a mitigation which originated from OpenBSD (originally named tame).
It enables a program to voluntarily restrict its access to the kernel's syscall
surface area. The allows the program to reduce the potential attack surface
available if the program in question was exploited.

It was first added in the following commit, and the majority of programs were enlightened later:

```
commit 41c504a33becea8aa9b437cd3c0dc312b2bf1fe9
Author: Andreas Kling <awesomekling@gmail.com>
Date:   Sat Jan 11 20:45:51 2020 +0100

Kernel: Add pledge() syscall :^)
```

### Unveil

[unveil](https://lwn.net/Articles/767137/) is a mitigation which originated from OpenBSD.
It enables a program to voluntarily restrict its access to the filesystem.
This reduces the potential surface area available if the program in question was exploited.

It was first added in the following commit, and the majority of programs were enlightened later:

```
commit 0569123ad7cb9c54df724c2bb85933ea3cf97134
Author: Andreas Kling <kling@serenityos.org>
Date:   Mon Jan 20 22:12:04 2020 +0100

Kernel: Add a basic implementation of unveil()
```
### syscall call-from verification

[syscall call-from verification](https://marc.info/?l=openbsd-tech&m=157488907117170&w=2) is
a mitigation which originated from OpenBSD.
In short the kernel checks that all syscalls originate
from the address of the systems libc. This makes attacks
on OpenBSD more difficult as they random-relink their libc
on boot, which makes finding syscall stubs in libc difficult
for attackers. On serenity it is mostly just an inconvenience,
as there currently is no libc random-relinking.

It was first enabled in the following commit:

```
commit 823186031d9250217f9a51829d34a96b74113334
Author Andreas Kling <kling@serenityos.org> 
Date:  Tue Feb 2 19:56:11 2021 +0100

Kernel: Add a way to specify which memory regions can make syscalls
```

### Post-init read-only memory

[Post-init read-only memory](https://lwn.net/Articles/666550/) is
a mitigation which originated from the Linux Kernel.
It tracks data that is initialized once during kernel boot and never
touched again, post kernel initialization the memory is marked
read only to protect it from potentially being modified by exploits.

It was first enabled in the following commit and other kernel data structures were enlightened later:

```
commit d8013c60bb52756788e747183572067d6e3f204a
Author: Andreas Kling <kling@serenityos.org>
Date:   Sun Feb 14 17:35:07 2021 +0100

Kernel: Add mechanism to make some memory read-only after init finishes

```

### KUBSAN (Kernel Undefined Behavior Sanitizer)

Undefined behavior sanitizer is a dynamic analysis tool, implemented in GCC,
which instruments generated code to flag undefined behavior at runtime.
It can find various issues including, overflows, out of bounds array
accesses, type corruption, and many more. Undefined behavior bugs can often
be exploited, KUBSAN allows developers to catch them during testing instead.

It was first enabled in the following commit:
```
commit d44be968938ecf95023351a358c43c4957638d87
Author: Andreas Kling <kling@serenityos.org>
Date:   Fri Feb 5 19:44:26 2021 +0100

Kernel: KUBSAN! (Kernel Undefined Behavior SANitizer) :^)
```

## See also

* [`unveil`(2)](../man2/unveil.md)
* [`pledge`(2)](../man2/pledge.md)
