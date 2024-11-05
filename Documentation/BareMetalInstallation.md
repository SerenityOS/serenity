# Serenity installation guide

## DISCLAIMER

Whilst it is possible to run Serenity on physical x86-compatible hardware, it is not yet ready to be used by non-technical users who aren't prepared to report bugs or assist with its development. For this reason, there are currently no pre-built install images so a bare-metal installation requires that you build an installation image from source. Current hardware support is extremely limited. Most successful hard disk installations have been on Pentium 4 era hardware but by network booting Serenity (which is no longer supported) users have been able to get it running on more modern hardware such as Core i5 machines.

## Hardware support and requirements

Storage-wise Serenity requires a >= 2 GB parallel ATA or SATA disk for IDE/AHCI. Some older SATA chipsets already operate in IDE mode whilst some newer ones will depend upon adjusting a BIOS option to run your SATA controller in IDE (sometimes referred to as Legacy or PATA) mode. SATA AHCI is supported, but may not work on every controller due to bugs in the implementation.
NVMe drives are supported but it is recommended to use `nvme_poll` boot parameter in newer hardwares at the moment. SCSI, SAS and eMMC HBAs are all presently unsupported.

You must be willing to wipe your disk's contents to allow for writing the Serenity image so be sure to back up any important data on your disk first! Serenity uses the GRUB2 bootloader so it should be possible to multiboot it with any other OS that can be booted from GRUB2 post-installation.

Serenity currently has no support for USB but some machines will emulate PS/2 keyboards and mice in the BIOS via USB. BIOS USB PS/2 emulation can be buggy so having real PS/2 input devices is recommended. A minimum of 256 MB RAM and a x86-64 CPU are required.

At present there is no real GPU support so don't expect OpenGL, Vulkan nor accelerated video playback and encoding support. Serenity currently relies upon VESA BIOS extensions to provide its display output and so it only runs on BIOS-based PCs. There is no WiFi support and the network card chipsets that are currently supported: Intel e1000, Intel e1000e and Realtek 8168. The e1000 driver has only been tested with qemu and VirtualBox although it may work with NICs such as those using the Intel 82545XX, 82540XX, 82546XX or similar chipsets. Supported sound cards are Intel AC'97 and Intel HDA PCI devices.

For more details on known working hardware see the [SerenityOS Hardware Compatibility List](HardwareCompatibility.md).

## Creating a Serenity GRUB disk image

Before creating a Serenity disk image, you need to build the OS as described in the [SerenityOS build instructions](BuildInstructions.md). Follow those instructions up to and including running **ninja install** (`Meta/serenity.sh image <arch>`). After the OS has built, run **ninja grub-image** to create a new file called **grub_disk_image** with GRUB2 installed that can be booted on a real PC. This command requires `parted` and `grub2` (Arch: `grub`) to be installed.

The final step is copying **grub_disk_image** onto the disk you wish to use to boot Serenity using a command such as:

```
$ sudo dd if=grub_disk_image of=/dev/sdx bs=64M && sync
```

Replace **/dev/sdx** with the target device. The **bs=64M** argument is optional but will speed up the data transfer. You can also use any other image flashing application. Flashing under Windows is possible; you can find the WSL files under `\\wsl$\<distro name>\<path to serenity directory>`.

## Troubleshooting Serenity boot issues with Linux using a null modem (serial) cable

Many guides on the internet recommend using `screen` to monitor or interact with a serial console under Linux. Using `screen` is an option but it is quite tricky to copy and paste the output from a `screen` console when there is more than one screens worth of text. So, unless you are already experienced with `screen` it is recommended you use `cu`.

After installing `cu`, you will not be able to connect to your serial console device until you have added your user to the **dialout** group. You must log out and log back in again after running a command such as:

```
$ sudo usermod -aG dialout YourLinuxUserName
```

Once you are logged in with a user who is a member of the **dialout** group, you can connect to a USB serial console using a command like:

```
$ cu -s 57600 -l /dev/ttyUSB0
```

## Troubleshooting boot issues without a serial port

During the boot process, you should be able to see logging of important messages on the screen, printed solely by the kernel.
If it happens to you that the system hangs, you should be able to see the last message on the screen. It can be either
an assertion or kernel panic. Depending on your hardware setup, the framebuffer could be 80x25 VGA text mode, or high resolution
framebuffer with 8x8 font glyphs.

You can force capable multiboot bootloaders to boot Serenity into high resolution mode by editing **Kernel/Arch/i386/Boot/boot.S** and
adding **| MULTIBOOT_VIDEO_MODE** to the end of the **multiboot_flags** before building Serenity.

Setting a boot argument of `graphics_subsystem_mode=limited` will force the kernel to not initialize any framebuffer devices, hence allowing the system to boot into console-only mode as `SystemServer` will detect this condition and will not initialize `WindowServer`.

If you do not see any output, it's possible that the Kernel panics before any video is initialized. In that case, you might try debugging the init sequence with the PC speaker to see where it gets stuck.
