## Name

Kernel cmdline - boot arguments to control system functionality

## Description

The kernel supports boot arguments to change some of its behavior. This
functionality can be used to disable or enable features in the OS entirely, but
can sometimes be used also to change a feature's behavior without disabling it
entirely.

Some of the boot arguments require a value after it. For those that don't
require a value, you should just write them without equals sign.

## Where to put those arguments

You can either add kernel boot arguments when you invoke `./run`.

You may wish to write the kernel boot arguments in a more consistent method. For
that, you can modify the GRUB configuration files. Since we have 3 of them (one
for each partition scheme), you should pick the right one for you, so if you
build a virtual disk with a GPT partition scheme, you should edit `grub-gpt.cfg`
file.

## Arguments

Since it's inconvenient for beginners to browse the source code (e.g. the
`init.cpp` file), the following arguments are recognized:

### Arguments that don't require a value

#### `text_debug`

This boot parameter enables Shell mode during boot, therefore you won't boot
into the graphical mode.

#### `no_vmmouse`

This boot parameter disables the VMMouse feature. This boot parameter is useless
on real hardware or emulators that don't support the VMMouse feature.

When specified in the boot cmdline when using QEMU, the PS/2 mouse packets are
position-relative (the X, Y & Z are actualy delta X, delta Y, delta Z) instead
of position-absolute.

#### `noacpi`

This boot parameter disables ACPI entirely. Therefore, the OS won't be able to
use power management features, nor to detect hardware that is only detectable
with ACPI.

#### `noacpi_aml`

This boot parameter disables ACPI to some extent. Therefore, the OS won't be
able to use power management features, nor to detect hardware that is only
detectable with ACPI. However, since this boot parameter only disables AML
interpretation, it is still possible to use some of ACPI features.

#### `dmi_unreliable`

This boot parameter warns the OS that the DMI (SMBIOS) implementation on the
machine is not reliable and the DMI decoder should be more careful when
interpreting the SMBIOS tables. For now this boot parameter has no real effect,
but in the future it can have.

#### `force_pio`

This boot parameter disables the DMA mechanism for the IDE controller. It is
meant that that this boot parameter will be used for debugging, when one
suspects that the IDE controller doesn't configure the DMA mechanism like we
expect it should.

#### `nopci_mmio`

This boot parameter disables the PCI Enhanced Configuration Access Mechanism.
Therefore, the kernel will fallback to access the PCI configuration space
through x86 IO ports, which are slower to use. This parameter is meant to be
used for debugging purposes, and has no effect on machines that don't support
PCI ECAM (e.g. i440FX machines and other old chipsets).

#### `serial_debug`

This boot parameter enables serial output via the traditional RS232 sockets.
This boot parameter is very useful for debugging on real hardware, since you may
have no any graphical output on the screen when the kernel halts due to some
error.

### Arguments that require a value

#### `root`

This boot parameter is specified to mount a partition the root filesystem.

On GPT or MBR schemes that have Extended partitions, there's no theoretical
limit on partition number.

to boot the first hard drive available by the IDE controller, partition 5.
However, on pure MBR partition scheme (no DOS extended partitions are present),
you may only specify a partition number between 1 to 4.

Please note, the implementation allows to boot only from the first IDE hard
drive currently. Also, that DOS extended partitions are limited by the kernel to
128 partitions. Such configurations are extremely rare in practice. GPT
partition scheme enforces a limit of about 4 billion partitions on one physical
media.

## Examples

### Boot arguments with a value

```
param1=value ...
```

### Boot arguments without a value

```
param1 ...

```

### `./run` with boot arguments (i440FX chipset)

```
./run qcmd param1 param2 ...
```

### `./run` with boot arguments (Q35 chipset)

```
./run q35_cmd param1 param2 ...
```

### `root` boot argument

```
root=/dev/hda5 param2 ...
```

## See also

* [`vmware_backdoor`(7)](vmware_backdoor.md)
