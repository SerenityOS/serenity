# Serenity installation guide

## DISCLAIMER

Whilst it is possible to run Serenity on physical x86-compatible hardware, it is not yet ready to be used by non-technical users who aren't prepared to report bugs or assist with its development. For this reason, there are currently no pre-built install images so a bare-metal installation requires that you build an installation image from source under Linux. Any Linux distribution capable of running GCC 8 (or newer) should be sufficient to build Serenity. Current hardware support is extremely limited.


## Hardware support and requirements

Storage-wise Serenity requires a >= 500 MB parallel ATA or SATA IDE disk. Some older SATA chipsets already operate in IDE mode whilst some newer ones will depend upon adjusting a BIOS option to run your SATA controller in IDE (sometimes referred to as Legacy or PATA) mode. SATA AHCI, SCSI, SAS, eMMC and NVME are all presently unsupported. 

You must be willing to wipe your disk's contents to allow for writing the Serenity image so be sure to back up any important data on your disk first! Serenity uses the GRUB2 bootloader so it should be possible to multiboot it with any other OS that can be booted from GRUB2 post-installation.

Serenity currently has no support for USB but some machines will emulate PS/2 keyboards and mice in the BIOS via USB. BIOS USB PS/2 emulation can be buggy so having real PS/2 input devices is recommended. A minimum of 32 MB RAM and a Pentium III class CPU are required.

At present there is no real GPU support so don't expect OpenGL, Vulkan nor accelerated video playback and encoding support. Serenity currently relies upon VESA BIOS extensions to provide its display output and so it only runs on BIOS-based PCs. There is no WiFi support and the only physical network card chipset currently supported is the RTL8139. The sole sound card supported is the SoundBlaster 16.

## Creating a Serenity GRUB disk image

Before creating a Serenity disk image, you need to build the OS as described in the [SerenityOS build instructions](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md). Follow those instructions up to and including running **./makeall.sh**. After the OS has built, run **sudo ./build-image-grub.sh** to replace the qemu **_disk_image** file in the Kernel directory with one that has GRUB2 installed that can be booted on a real PC. Alternately, you can run **sudo ./build-gpt-image-grub.sh** to create an image that uses GPT partitioning. 

The final step is copying **_disk_image** onto the disk you wish to boot Serenity off using a command such as:

```
$ sudo dd if=_disk_image of=/dev/sdx bs=8M
```

Replace **/dev/sdx** with the target device. The **bs=8M** argument is optional but will speed up the data transfer.

Serenity doesn't output any kernel boot messages to the display device so if it fails to boot you will need a serial port and a null modem cable to discover the cause of the failure.

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

Serenity is a graphical OS and thus boots directly to the desktop. If your computer doesn't have a serial port and it fails to boot, you can force Serenity to boot into text mode by editing **Kernel/Arch/i386/Boot/boot.S** and removing **| MULTIBOOT_VIDEO_MODE** from the end of the **multiboot_flags** line before (re)running **makeall.sh**.
