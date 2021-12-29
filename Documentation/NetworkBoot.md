# SerenityOS network booting via PXE

- [SerenityOS network booting via PXE](#serenityos-network-booting-via-pxe)
  - [What's network booting?](#whats-network-booting)
- [PXE firmwares](#pxe-firmwares)
  - [Built-in network card PXE firmware](#built-in-network-card-pxe-firmware)
  - [iPXE](#ipxe)
- [DHCP/TFTP servers](#dhcptftp-servers)
  - [QEMU's integrated DHCP/TFTP servers](#qemus-integrated-dhcptftp-servers)
  - [dnsmasq](#dnsmasq)
- [Bootloaders](#bootloaders)
  - [GRUB2](#grub2)
  - [iPXE](#ipxe-1)
  - [PXELINUX](#pxelinux)
- [Troubleshooting](#troubleshooting)

## What's network booting?

Network booting is the process that a target computer (physical or virtual) follows in order to boot over the network through the preboot execution environment, or PXE.
Network booting is composed of several stages:
- The PXE firmware starts on the target computer and queries the network for a boot file,
- The DHCP server gives a lease to the PXE firmware with options for the IP address of the TFTP server and the path of the boot file to load,
- The TFTP server supplies the boot file and any additional files to the PXE firmware,
- The bootloader eventually loads the operating system and boots into it.

You need to pick one of each in order to achieve network booting.

For simplicity, this guide assumes several things:
- The bootloader, kernel and root file system are all supplied over the network instead of local media (diskless boot)
- The TFTP server root is located at `/srv/tftp/`
- Bootloaders are located inside `/srv/tftp/boot/${BOOTLOADER}/`:
  - GRUB2 files are located inside `/srv/tftp/boot/grub`
  - PXELINUX files are located inside `/srv/tftp/boot/pxelinux`
- SerenityOS artifacts are located inside `/srv/tftp/serenity/`:
  - The prekernel is located at `/srv/tftp/serenity/prekernel`
  - The kernel is located at `/srv/tftp/serenity/kernel`
  - The ramdisk is located at `/srv/tftp/serenity/ramdisk`

You can find the SerenityOS artifact at the following locations:
  - The prekernel is located at `Build/${SERENITY_ARCH}/Kernel/Prekernel/Prekernel`
  - The kernel is located at `Build/${SERENITY_ARCH}/Kernel/Kernel.drow`
  - While the SerenityOS build system cannot generate a dedicated ramdisk yet, you can use the QEMU disk image at `Build/${SERENITY_ARCH}/_disk_image` as a ramdisk

# PXE firmwares

## Built-in network card PXE firmware

Virtual and physical network cards generally have built-in PXE capability, but by default they are likely to be the least prioritized boot option, if enabled at all.
Make sure that the target computer will attempt to boot over the network instead of booting from local media.

Unless you are dealing with very old hardware or buggy PXE stacks, this is generally the best option. Note that SerenityOS doesn't have to support your network card in order to boot from it.

## iPXE

iPXE can be used to provide PXE capability to a computer by booting it from local media.
The prebuilt [ISO image](http://boot.ipxe.org/ipxe.iso), once written to a CD-ROM, hard drive or USB mass storage device, can be directly used as a drop-in replacement for a missing or broken network card PXE stack.

# DHCP/TFTP servers

There are many DHCP and TFTP server implementations and your local network probably already has a DHCP server running.
Refer to the documentation of your network installation/operating system for more details on how to properly setup a network booting environment.

## QEMU's integrated DHCP/TFTP servers

QEMU can emulate a variety of network services in user mode networking, all provided through the built-in gateway, including the required services for achieving network boot.

Sample command to start a QEMU virtual machine with built-in DHCP/TFTP services configured for network booting:

```qemu-system-i386 -m 4096 -netdev user,tftp=/srv/tftp,bootfile=boot/${BOOTLOADER}/${BOOTFILE},id=network0 -device e1000,netdev=network0```

## dnsmasq

`dnsmasq` can act as both a DHCP and a TFTP server.

1. Install the required packages on the TFTP server:
   - Debian and Ubuntu: `sudo apt install dnsmasq`
2. Make sure `/srv/tftp/` is owned by the user `tftp`, otherwise the TFTP server won't serve files
3. Create a dnsmasq configuration file at `/etc/dnsmasq.conf`
4. Start dnsmasq by running `sudo systemctl start dnsmasq`

Sample `dnsmasq.conf` configuration file:

```
# Interface to use to provide DHCP and TFTP
interface=eth0

bind-interfaces

# Set default gateway
dhcp-option=3,192.168.0.1

# Set DNS servers to announce
dhcp-option=6,1.1.1.1

# If using IPV4, dnsmasq can coexist alongside another DHCP server
# by using the proxy command with dhcp-range instead.
dhcp-range=192.168.0.10,192.168.0.222,12h

# Don't function as a DNS server.
port=0

# Log information about DHCP transactions.
log-dhcp

# Set the root directory for files available via FTP,
tftp-root=/srv/tftp

enable-tftp

# The boot filename, Server name, Server Ip Address
dhcp-boot=boot/grub2/i386-pc/core.0,,192.168.0.7
```

# Bootloaders

## GRUB2

1. Create a netbootable directory with `grub-mknetdir --net-directory=/srv/tftp --subdir=boot/grub`
2. Create a GRUB2 configuration file at `/srv/tftp/boot/grub/grub.cfg`
3. Configure your DHCP server to serve `boot/grub/i386-pc/core.0` as your boot file

Note: the `grub-pc-bin` package, which contains the BIOS modules for PXE booting GRUB2, isn't available from the ARM repos of Debian and Ubuntu. If you are using an ARM machine for your TFTP server, you will need to extract and copy across the contents of the `/usr/lib/grub/i386-pc/` directory from the x86 package or build the files manually.

Sample `grub.cfg` configuration file:
```
set gfxmode=auto
insmod all_video
insmod gfxterm
terminal_output gfxterm

menuentry 'SerenityOS - netboot diskless graphical mode' {
        echo 'Loading prekernel...'
        multiboot (tftp)/serenity/prekernel root=/dev/ramdisk0
        echo 'Loading kernel...'
        module (tftp)/serenity/kernel
        echo 'Loading ramdisk...'
        module (tftp)/serenity/ramdisk
        echo 'Starting SerenityOS.'
}

menuentry 'SerenityOS - netboot diskless text mode' {
        set gfxkeep=text
        terminal_output console
        echo 'Loading prekernel...'
        multiboot (tftp)/serenity/prekernel root=/dev/ramdisk0 fbdev=off
        echo 'Loading kernel...'
        module (tftp)/serenity/kernel
        echo 'Loading ramdisk...'
        module (tftp)/serenity/ramdisk
        echo 'Starting SerenityOS.'
}
```

## iPXE

Warning: the default build of iPXE cannot set up a framebuffer for Multiboot targets, unless your graphical card is supported by SerenityOS you will most likely have no graphics on real hardware.

iPXE can directly boot SerenityOS with its `kernel` and `module` commands. This requires either setting the DHCP vendor-specific option `ipxe.scriptlet` to the script's URL, building a custom iPXE image with an embedded script or typing in commands through the interactive prompt (Ctrl-B). Refer to the official documentation at https://ipxe.org for more information.

Sample iPXE embedded script:
```
#!ipxe
dhcp
kernel serenity/prekernel root=/dev/ramdisk0
module serenity/kernel
module serenity/ramdisk
boot
```

## PXELINUX

Warning: PXELINUX cannot set up a framebuffer for Multiboot targets, unless your graphical card is supported by SerenityOS you will most likely have no graphics on real hardware.

1. Install required packages on the TFTP server
   - Debian: `sudo apt install pxelinux`
2. Copy all bootloader modules (located inside `/usr/lib/PXELINUX/` and `/usr/lib/syslinux/modules/bios/` on Debian) inside `/srv/tftp/boot/pxelinux`
3. Put your `default` configuration file inside `/srv/tftp/boot/pxelinux/pxelinux.cfg/`
4. Configure your DHCP server to serve `boot/pxelinux/lpxelinux.0` as your boot file

Sample `default` configuration file:

```
UI vesamenu.c32

LABEL SerenityOS
        KERNEL mboot.c32
        APPEND serenity/prekernel root=/dev/ramdisk0 --- serenity/kernel --- serenity/ramdisk
```

# Troubleshooting

It is recommended to netboot a mainstream operating system on the target at least once before attempting to netboot SerenityOS, otherwise it may be difficult to isolate an issue with SerenityOS from an issue with your network booting setup.

- Ensure your paths on the TFTP server and the DHCP options are correct.
- Check that your TFTP server is properly working with a TFTP client. Wrong file permissions or ownership on `/srv/tftp` can prevent the TFTP server from functioning properly.
- TFTP is a slow protocol, big files can take tens of seconds or even minutes for the transfer to finish. Consider setting up a FTP or HTTP server for faster downloading of SerenityOS artifacts if your bootloader supports it.
- Remember that SerenityOS has not been extensively tested on physical hardware, the kernel may fail to boot on your target computer for reasons unrelated to network booting.
- Try using iPXE if you suspect your network card's built-in PXE firmware is buggy or if the target computer doesn't have a PXE boot option.
- Virtual machines can also be booted over the network. Cheat notes for QEMU on Linux, assuming `br0` is already set up:

```
ip tuntap add tap0 mode tap user <username>
ip link set tap0 master br0
ip link set tap0 up

echo 0 > /proc/sys/net/bridge/bridge-nf-call-iptables
echo 0 > /sys/devices/virtual/net/br0/bridge/multicast_querier

qemu-system-i386 -m 4096 -netdev tap,ifname=tap0,script=no,downscript=no,id=network0 -device e1000,netdev=network0 -boot n -debugcon stdio -s
```
