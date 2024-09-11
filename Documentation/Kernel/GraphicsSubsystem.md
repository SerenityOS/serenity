# Introduction to the Kernel Graphics Subsystem

## What is the Kernel Graphics Subsystem?

The Kernel Graphics Subsystem is the kernel subsystem that is responsible to
manage all graphics devices, framebuffers, hardware 3D acceleration, memory mappings, etc.

## Responsibilities

-   Provide a convenient interface to all supported video hardware in the Kernel.
-   Manage 3D rendering on supported hardware.

## Current Limitations and Future features?

-   No locking on who can do `mmap` on DisplayConnector devices currently, which can
    lead to malicious applications "fighting" with WindowServer on what is shown to the user
    from the framebuffer.

# DisplayConnector Devices

The Display Connector devices are an abstraction layer to what is essentially the
management layer of hardware display (commonly known as scanouts) output connectors.
The idea of using such type of device was inspired by Linux, which has a struct called
`drm_connector` as a base structure for other derived structures in the various Linux DRM drivers.

A Display connector device is typically connected to a group of other connectors as well,
as it's generally common to have video hardware that utilizes multiple hardware connectors
to VGA, DisplayPort, HDMI, DVI, etc. However, it can be a stand-alone device too, which
is the case for the `GenericDisplayConnector` class, that can be initialized without being
attached to a parent PCI device object at all.

Each display connector is programmatically accessible via a device file, in the
`/dev/gpu/` directory with a name `connectorX` (X is replaced with the minor number).

Each display connector could be `mmap`-ed to gain control to video RAM directly.
This works nicely with the kernel TTY subsystem thanks to the role of virtual memory
in the subsystem.

# Hardware framebuffers

## History lesson on ISA, PCI, VGA (and SVGA)

Since the beginning of video hardware with old-school ISA VGA display adapters,
there was a window being mapped in the physical address space, being translated
by the motherboard chipset as read/write to video RAM. When SuperVGA came along
in the 90s, it expanded the usage of that small VGA window (where it was in very low memory)
to high resolution framebuffers in very high memory regions. This tradition continues today
to some extent (excluding hardware which requires DMA from main memory to video memory),
because it's relatively cheap and easy way to let operating systems to access video RAM
directly without too much trouble.

Since the main PC x86 computer bus was the IBM ISA bus, there was no easy way to tell where
the resources of each card were actually located at the IO space nor in the physical memory space.
There were a couple of attempts to fix this - the most notable was the Plug-and-Play standard.

The real change came from a new computer bus in the mid 90s - the PCI bus. This new bus
was PnP friendly - no more hardcoded resource allocations which means also that OS drivers
can find where the firmware (BIOS) mapped the BAR (Base address registers) for the actual resources.
This was also the era where SuperVGA video adapters started to appear, taking advantage of this
new bus.

Since VGA was introduced, countless amount of vendors brought their own implementations
and video adapters for usage in the PC market. By now, most of them are gone, leaving the major
vendors (Intel, AMD and Nvidia) to still be able to manufacture video adapters which are today
commonly known as Graphics Processing Unit (abbreviated as GPU) - due to the fact that today
video adapters are not only outputting pixels to the computer screen, but have a whole set of processors
to take care of heavy computational tasks of graphics assets, and even general processing tasks nowadays.

SuperVGA was only the first step into this direction, yet SuperVGA is not a standard, but
a marketing name for is essentially each video adapters' vendor tried to do in the 90s -
building an extension upon VGA. All of these vendors did that without creating a unified standard,
like with VGA, which ensured everyone are conforming to well-known and expected video hardware behavior.
To try to cope with the dire situation, the VBE (Video BIOS extensions) standard was created to
help BIOS and operating system vendors to be able to get high resolution framebuffer from
any hardware that complied to the standard. When UEFI came along, the vendors agreed
to create the Graphics output protocol (known as UEFI GOP), to provide the same set of features
that VBE had, but now is usable from 64-bit kernel code as long as the kernel didn't shutdown
the UEFI services (which it really should do) after completing the boot process.

## Then how does it all apply to the subsystem?

Glad you asked! Since hardware framebuffers are still relevant today, we use them
to put pixels so the video encoder of a GPU can convert these bits into light, so
you could actually see a picture from a computer screen. Each GPU implements its own
internal functionality so it might vary from very simple devices (like the QEMU bochs-display
device, which is nothing more than framebuffer region and a couple of registers to manage it)
to very complex devices, such as bare metal devices (like Intel integrated GPUs, etc).

The Kernel graphics subsystem strives to manage all of these devices in a unified fashion
as much as possible. Of course, actual implementations should vary internally in the
amount of code to handle the actual device, but all basic API being exposed to userspace is the same.

## The role of MMUs and virtual memory

One of the primary goals of the subsystem to is to allow userspace applications,
like the WindowServer, to utilize the hardware framebuffers, so we can see the SerenityOS
desktop, and to ensure the internal TTY subsystem in the Kernel can use the same framebuffers
to put output from kernel virtual consoles when desired to (i.e. the user switched to the
Virtual console from another console that is in graphics mode).

The SerenityOS kernel utilizes the MMU and virtual memory in a very neat trick to
give the "feel of control" to whoever did the `mmap` syscall on a DisplayConnector
device, while keeping the control to the Kernel to decide who accesses the actual VRAM
at a given time. This works by working with the following assumptions:

1. Current usage of `mmap` is only for direct framebuffer manipulation. This means
   that if we add support for batch buffers or other objects that should reside in VRAM, this trick
   can lead to catastrophic incidents with the underlying hardware. This happens to be this way, due to
   the fact that we essentially can take VRAM access from the WindowServer at anytime we want,
   without the WindowServer being aware of this so it can still function in the background.
2. We need to know the maximum dimensions of the framebuffers when initializing the device
   and creating the DisplayConnector device at runtime. This happens because we map all the possible
   pages of VRAM framebuffer at that time, and also reserve the same amount of pages in usable
   physical memory space, so we could reserve the contents of VRAM between the switch
   from graphics mode to console mode and vice-versa.

The actual implementation is quite simple, yet powerful enough to let everyone
live comfortably - each DisplayConnector device is backed by a special VMObject (VMObject is
the base class for managing virtual memory scenarios easily) that is created when the
DisplayConnector device is initialized - we need to find the physical address of the
start of the framebuffer and the maximum resource size (this is where PCI BARs play their role,
as we can determine with them the physical address by reading their values and also
the maximum resource size, by doing a very simple write 1s-and-read trick that was introduced
with the PCI bus when it was created). Then when the object is created, the code ensures
we reserve for later usage the same amount of pages somewhere else to ensure we preserve
the contents of VRAM between the switch from console and graphics mode and vice-versa.
The special VMObject is tied to each `Memory::Region` object, so it can instruct each
virtual-to-physical memory mapping to be actually re-mapped to wherever we want in physical
address space, therefore, we do not interrupt any userspace application from drawing its pixels
to the framebuffer in the background.

## Do you plan supporting old VGA adapters?

Given the nature of the user experience SerenityOS strives to deliver to the users,
a core requirement from the first day of this project was to only support 32 bit-per-pixel
(also known as True-color framebuffer) hardware framebuffers. We do support hardware
framebuffers that neglect the alpha-channel (essentially it's a 24 bit-per-pixel),
as long as each pixel is aligned to 4 bytes. The QEMU std-vga (bochs-display with
VGA capabilities) device was chosen as the first device to be supported in the project,
and that was an excellent choice for that time to put up with the said requirement.

This hard requirement is due to the fact that supporting anything besides True-color
framebuffers is a _waste of time_ for a new modern kernel. Not only that, but relying
on VGA with modern monitors is essentially settling for blurry, badly-shaped graphics
on a computer monitor, due to unoptimized resolution scaling with modern screen ratios.

Old VGA adapters are certainly not capable of using high resolution framebuffers
when operating in pure native VGA mode (i.e. not operating in an extension mode
of the video adapter), therefore, if the Kernel cannot find a suitable framebuffer
to work with or a video adapter it has a driver for, then the last resort is to use the old VGA text mode
console. Therefore, the SerenityOS kernel will probably never support pure VGA functionality.
That technology was good for operating systems in the 90s, but is not usable anymore.

By doing so, we ensure that legacy cruft is not introduced in the Kernel space. This indeed
helps keeping the Graphics subsystem lean and flexible to future changes.

## What about the Video BIOS Extensions? It can gives high resolution framebuffers without writing native drivers!

As for using Video BIOS extensions - this requires us to be able to call to BIOS 16-bit real mode
code. The solutions for these are:

1. Drop to real mode, invoke the BIOS interrupt and return to our kernel.
2. Writing a Real-Mode 16-bit emulator, either in Kernel space or userspace.
3. Use Intel VT-x extensions to simulate a processor running in Real mode.
4. Use the old v8086 mode in x86 processors to get an hardware monitor of 16-bit tasks.

Neither of these options is suitable for us. Dropping to real mode is quite dangerous task, and breaks
the concept of memory protection entirely. Writing a real mode emulator is the safest solution, yet can
take a not negligible amount of effort to get something usable and correct. Using the hardware options
such as Intel VT-x or the v8086 mode are almost equally equivalent to writing an emulator.

We will probably never support using the Video BIOS extensions because of these reasons:

1. Major part of this project is to maximize usability and fun on what we do, and turning into legacy-cruft to
   temporarily solve a solution is not the right thing to do.
2. VBE is not usable on machines that lack support of BIOS. As of 2022, this increasingly becomes a problem
   because many PC vendors dropped support for BIOS (known as CSM [Compatibility Support Module] in UEFI terms).
3. VBE is limited to whatever the vendor decided to hardcode in the OptionROM of the video adapter, which means
   it can limit us to a small set of resolutions and bits-per-pixel settings,
   some of these settings are not convenient for us, nor suitable for our needs.
4. VBE lacks the support of detecting if the screen actually supports the resolution settings,
   which means that the operating system has to use other methods to determine if screen output is
   working properly (e.g. waiting for a couple of seconds for user confirmation on the selected settings).
   This is because VBE lacks support of getting the screen EDID because most of the time,
   the EDID resides in a ROM in the computer screen, which is inaccessible without using specific
   methods to extract it (via the Display Data Channel), which are not encoded or implemented in
   the PCI OptionROM of the device.
   This is in contrast to native drivers which are able to do this, and VGA, that never relied on
   such methods and instead relied on all video adapters and computer screen to use an well-known
   specification-defined display modes.

## What are the native drivers that are included in the kernel? what type of configurations are supported?

The kernel can be configured to operate in the following conditions:

1. Fully-enable the graphics subsystem, initialize every device being supported.
2. Only use the pre-initialized framebuffer from the bootloader, don't initialize anything else.
3. Don't use any framebuffer, don't initialize any device.

By default, we try to fully-initialize the graphics subsystem, which means we iterate
over all PCI devices, searching for VGA compatible devices or Display Controller devices.

We currently natively support QEMU std-vga (and bochs-display) device, VirtIO GPU, VMWare SVGA II adapter,
and Intel Graphics (Gen 4 only). We try our best to avoid using a pre-initialized framebuffer, so
if we detect any of the said devices, we simply ignore the pre-initialized framebuffer from the bootloader.

The user can choose to use a different condition of the Graphics subsystem, but hardware limitations
such as lack of supported hardware can either lead the Kernel to use a pre-initialized framebuffer
or completely "abandon" graphics usage (as was mentioned in third condition), making the system usable
only through a VGA 80x25 text mode console.

# Userspace APIs

## Unified Graphics IOCTLs

All graphics ioctls are currently unified and being implemented in one final method
of the `DisplayDevice` class, to keep implementation consistent as much as possible.

## Syscalls

The `read` and `write` syscalls are not supported and probably will never be. In the transition
period from the old framebuffer code in the Kernel to the current design, the `mmap` syscall was
quite dangerous and did not handle multiple userspace programs trying to use it on one device.
Since that was resolved and `mmap` can be used safely, `read` and `write` syscalls are no longer
needed and are considered obsolete for this device because no userspace program in Serenity will
ever need to use them, or test them at the very least.

The `ioctl` syscall is used to control the DisplayConnector device - to invoke
changing of the current mode-set of a framebuffer, flush the framebuffer, etc.

## Major and minor numbering

The major number is fixed at 226. Minor number is allocated incrementally as instances
are initialized.
