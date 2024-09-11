# The IOWindow class

## Introduction to port-mapped IO and memory-mapped IO

### Port-mapped IO

Port mapped IO is a x86-specific method to access hardware registers. It uses a
set of specific instructions in the x86 architecture to invoke Input and Output operations
on hardware that is present in the platform board.

```c++
IOAddress io(0x3f0)
u8 ide_status = io.offset(0).in<u8>()
```

### Memory-mapped IO

Memory mapped IO is a platform-agnostic method to access hardware registers. It uses a
set of memory access instructions in many computer architectures to invoke Input and Output operations
on hardware that is present in the platform board.

```c++
auto mapping = Memory::TypedMapping::map_typed_writable<u16>(0xb8000);
*mapping = 0x001b;
```

## The `IOWindow` class to rule them all (almost)!

The entire idea behind the `IOWindow` class is to make it much more easier to compile
the Kernel for non-x86 builds, so the class abstracts platform-specific methods to access
hardware such as the port-mapped IO method. In compile-time, when generating a Kernel for
non-x86 target, the entire port-mapped IO code is omitted as it's not relevant for non-x86
targets.

In many cases, devices (such as PCI devices) can either use the IO space or memory space
to expose their registers for the CPU to utilize as the host software invokes IO operations
for various reasons. Some devices expose equivalent registers in both the IO space and memory space
to help legacy host software to interact with the hardware. One example to this is old AHCI controllers
which could be used in legacy mode - i.e. exposing SFF IDE registers in the IO space, or to enable memory
mapped registers as being defined in the SATA AHCI HBA specification.

The general rule in kernel driver programming is to know that there are only two valid
cases on whether to use the `IOWindow` structure or not:

1. The device is known to either use the IO space, memory space or both, taking into
   consideration that variants of the device can disable either of the options. In this case,
   we need to use the `IOWindow` structure as it will help us to correctly use the IO window
   in either case.
2. The device is known to use only the memory space, therefore we can ignore the `IOWindow`
   structure and instead use the `Memory::TypedMapping` structure to help navigating in
   the memory-mapped registers of the device.

# A note about 64 bit access for memory mapped IO

As far as we can tell, writing to 64 bit register can actually be done for the most part
with two 32 bit IO access operations. When genuine 64 bit access is needed, `IOWindow` is
probably not the appropriate solution anyway, because the device is only supporting memory-mapped IO
and there's no way it can provide register access via port mapped IO - that method simply doesn't
support generating 64 bit IO access, so you should use the `Memory::TypedMapping` mapping method instead.

Therefore, to ensure we keep everything simple, there's simply no API to generate pure 64 bit IO access with
the `IOWindow` class.
