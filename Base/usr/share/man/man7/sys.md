## Name

sys - SerenityOS SysFS

## Description

The kernel can expose system (kernel, firmware and hardware) related information in /sys.

### `bus` directory

This directory include a subdirectory for each discovered and registered bus in the system.

Possible busses to be exposed in this directory are:
1. The `pci` subdirectory that includes all discovered PCI devices as subdirectories.
The subdirectories of the PCI devices include files with basic information on the devices.
2. The `usb` subdirectory that includes all discovered USB devices as files.
The files of the USB devices export basic information on the devices.

### `dev` directory

This directory include two subdirectories - `block` and `char`, each for block
and character devices respectively. The files in these subdirectories are not
device files, but merely a file with filename layout of "major:minor", to aid
userspace in generating the appropriate device files.

### `firmware` directory

This directory include two subdirectories - `acpi` and `bios`.
The `bios` subdirectory maintains files of the exposed SMBIOS blobs, if present
by the firmware.
The `acpi` subdirectory maintains files of the exposed ACPI tables, if present
by the firmware.
A file called `power_state` is responsible for power state switching.

#### `power_state`

This file only responds to write requests on it. A written value of `1` results
in system reboot. A written value of `2` results in system shutdown.

### Consistency and stability of data across multiple read operations

When opening a data node, the kernel generates the required data so it's prepared
for read operation when requested to. However, in order to ensure that multiple reads
will not create a corrupted data from that data node, a read operation alone will
not inquire the kernel to refresh the data.
To keep data output being refreshed, the userland has to re-open the data node with a 
new file descriptor, or to perform the `lseek` syscall on the open file descriptor to
reset the the offset to 0.

## See also

* [`mount`(2))](help://man/2/mount).
