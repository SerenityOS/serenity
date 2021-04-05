# Serenity installation guide

## DISCLAIMER

Whilst it is possible to run Serenity on physical x86-compatible hardware, it is not yet ready to be used by non-technical users who aren't prepared to report bugs or assist with its development. For this reason, there are currently no pre-built install images so a bare-metal installation requires that you build an installation image from source. Current hardware support is extremely limited. Most successful hard disk installations have been on Pentium 4 era hardware but by network booting Serenity users have been able to get it running on much more modern hardware such as Core i5 machines.


## Hardware support and requirements

Storage-wise Serenity requires a >= 2 GB parallel ATA or SATA IDE disk. Some older SATA chipsets already operate in IDE mode whilst some newer ones will depend upon adjusting a BIOS option to run your SATA controller in IDE (sometimes referred to as Legacy or PATA) mode. SATA AHCI is supported, but may not work on every controller due to bugs in the implementation.
SCSI, SAS, eMMC and NVMe HBAs are all presently unsupported.

You must be willing to wipe your disk's contents to allow for writing the Serenity image so be sure to back up any important data on your disk first! Serenity uses the GRUB2 bootloader so it should be possible to multiboot it with any other OS that can be booted from GRUB2 post-installation.

Serenity currently has no support for USB but some machines will emulate PS/2 keyboards and mice in the BIOS via USB. BIOS USB PS/2 emulation can be buggy so having real PS/2 input devices is recommended. A minimum of 128 MB RAM and a Pentium III class CPU are required.

At present there is no real GPU support so don't expect OpenGL, Vulkan nor accelerated video playback and encoding support. Serenity currently relies upon VESA BIOS extensions to provide its display output and so it only runs on BIOS-based PCs. There is no WiFi support and the only three network card chipsets are currently supported: Realtek RTL8139, Novell NE2000 and Intel e1000. The e1000 driver has only been tested with qemu and VirtualBox although it may work with NICs such as those using the Intel 82545XX, 82540XX, 82546XX or similar chipsets. The sole sound card supported is the SoundBlaster 16 ISA.

For more details on known working hardware see the [SerenityOS Hardware Compatibility List](https://github.com/SerenityOS/serenity/blob/master/Documentation/HardwareCompatibility.md).

## Creating a Serenity GRUB disk image

Before creating a Serenity disk image, you need to build the OS as described in the [SerenityOS build instructions](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md). Follow those instructions up to and including running **make install**. After the OS has built, run **make grub-image** to create a new file called **grub_disk_image** with GRUB2 installed that can be booted on a real PC.

The final step is copying **grub_disk_image** onto the disk you wish to use to boot Serenity using a command such as:

```
$ sudo dd if=grub_disk_image of=/dev/sdx bs=64M && sync
```

Replace **/dev/sdx** with the target device. The **bs=64M** argument is optional but will speed up the data transfer.

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

If your computer fails to boot and it doesn't have a serial port, you can force Serenity to boot into text mode by editing **Kernel/Arch/i386/Boot/boot.S** and removing **| MULTIBOOT_VIDEO_MODE** from the end of the **multiboot_flags** before building Serenity. This debug source tweak differs from the Serenity text mode GRUB boot option which boots you directly into a text mode shell.
