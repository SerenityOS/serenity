# Serenity installation guide for VMware

## NOTICE

There are currently issues with running Serenity in VMware. Please refer to the [open issue](https://github.com/SerenityOS/serenity/issues/5716) for a list of currently known issues. Anything that doesn't currently work will be noted in this document.

## Creating the disk image

Before creating a disk image that will work in VMware, you will need to create a GRUB image as described in the [Serenity installation guide](BareMetalInstallation.md). Please skip the final step of that section, as that is only relevant for putting the image onto a real drive. You **cannot** use the same disk image created for QEMU. Using that image will halt immediately with the message `FATAL: No bootable medium found! System halted.`

The easiest way to convert the disk image is with QEMU:

`qemu-img convert -O vmdk /path/to/grub_disk_image /path/to/output/serenityos.vmdk`

## Creating the virtual machine

Creating a SerenityOS virtual machine is similar to any other virtual machine. The main difference is using the already created VMDK disk image.

**Please note that these instructions were written with VMware Player 15 in mind. Therefore, these instructions may not match exactly for past and future versions or VMware Workstation.**

1. Open the **Create a New Virtual Machine** dialog. Select **I will install the operating system later**.
2. Choose **Other** as the guest operating system.
3. Feel free to give it any name and store it anywhere.
4. Choose any size for the hard disk. This disk will later be removed and replaced with the converted GRUB image from the previous stage.
5. Select **Finish** to finalize creation of the virtual machine.
6. Select the newly created virtual machine and click **Edit virtual machine settings**.
7. Serenity requires at minimum 512 MiB of memory. Set **Memory for this virtual machine** equal to or above 512 MiB. The currently recommended size is 1 GiB.
8. Select the existing **Hard Disk** and click **Remove**.
9. Select **Add**, select **Hard Disk**, select **IDE (Recommended)**, select **Use an existing virtual disk**.
10. Click **Browse** and browse to where you stored the converted VMDK disk image from the previous stage and add it. Click **Finish**.
11. Finally click **Save**. You can now **Power On** the virtual machine.

Please note that at the time of writing, audio and networking do not work in VMware.

That is all you need to boot Serenity in VMware!
