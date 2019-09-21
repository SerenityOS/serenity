# Serenity installation guide

## DISCLAIMER

Whilst it is possible to run Serenity on physical x86-compatible hardware, it is not yet ready to be used by non-technical users who aren't prepared to report bugs or assist with its development. For this reason, there are currently no pre-built install images so a bare-metal installation requires that you build an installation image from source under Linux. Any Linux distribution capable of running GCC 8 (or newer) should be sufficient to build Serenity. Current hardware support is extremely limited.


## Hardware support

The main requirement for running Serenity on real hardware is having a parallel ATA IDE disk attached to your machine. You must be willing to wipe its contents to allow for writing the Serenity image so be sure to back up any wanted data on your disk first! Serenity uses the GRUB2 bootloader so it should be possible to multiboot it with any other OS that can be booted from GRUB2 post-installation.

The other important hardware requirement is PS/2 mouse and keyboard support. Serenity currently has no support for USB but some machines will emulate PS/2 keyboards and mice in the BIOS via USB. BIOS USB PS/2 emulation can be buggy so having real PS/2 input devices is recommended.

Serenity currently has no real GPU support so there is no OpenGL, Vulkan nor accelerated video playback and encoding support. There is no WiFi support and the only physical network card chipset currently supported is the RTL8139. The sole sound card supported is the SoundBlaster 16.

## Creating a Serenity GRUB disk image

Before creating a Serenity disk image, you need to build the OS as described in the **"How do I build and run this?"** section of the [Serenity ReadMe](https://github.com/SerenityOS/serenity/blob/master/ReadMe.md). Follow those instructions up to and including running **./makeall.sh**. After the OS has built, run **sudo ./build-image-grub.sh** to replace the qemu **_disk_image** file in the Kernel directory with one that has GRUB2 installed that can be booted on a real PC. 

The final step is copying **_disk_image** onto the disk you wish to boot Serenity off using a command such as:

```
sudo dd if=_disk_image of=/dev/sdx bs=8M
```

Replace **/dev/sdx** with the target device. The **bs=8M** argument is optional but will speed up the data transfer.

Serenity doesn't output any kernel boot messages to the display device so if it fails to boot you will need a serial port and a null modem cable to discover the cause of the failure.
