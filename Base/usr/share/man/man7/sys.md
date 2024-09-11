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

### `kernel` directory

This directory includes two subdirectories - `net` and `conf`.
All other files in the directory are global data nodes which provide statistics
and other kernel-related data to userspace.

#### `kernel` directory entries

-   **`processes`** - This node exports a list of all processes that currently exist.
-   **`cpuinfo`** - This node exports information on the CPU.
-   **`df`** - This node exports information on mounted filesystems and basic statistics on
    them.
-   **`dmesg`** - This node exports information from the kernel log.
-   **`interrupts`** - This node exports information on all IRQ handlers and basic statistics on
    them.
-   **`keymap`** - This node exports information on the currently used keymap.
-   **`memstat`** - This node exports statistics on memory allocation in the kernel.
-   **`profile`** - This node exports statistics on profiling data.
-   **`stats`** - This node exports statistics on scheduler timing data.
-   **`uptime`** - This node exports the uptime data.
-   **`power_state`** - This node only responds to write requests on it. A written value of `1` results
    in system reboot. A written value of `2` results in system shutdown.
-   **`load_base`** - This node reveals the loading address of the kernel.
-   **`system_mode`** - This node exports the chosen system mode as it was decided based on the kernel commandline or a default value.
-   **`cmdline`** - This node exports the kernel boot commandline that was passed from the bootloader.
-   **`request_panic`** - This node allows userspace to trigger (an artificial) kernel panic by writing/truncating it.

#### `net` directory

-   **`adapters`** - This node exports information on all currently-discovered network adapters.
-   **`arp`** - This node exports information on the kernel ARP table.
-   **`local`** - This node exports information on local (Unix) sockets.
-   **`tcp`** - This node exports information on TCP sockets.
-   **`udp`** - This node exports information on UDP sockets.

#### `conf` directory

This subdirectory includes global settings of the kernel.

-   **`caps_lock_to_ctrl`** - This node controls remapping of of caps lock to the Ctrl key.
-   **`kmalloc_stacks`** - This node controls whether to send information about kmalloc to debug log.
-   **`ubsan_is_deadly`** - This node controls the deadliness of the kernel undefined behavior
    sanitizer errors.

### Consistency and stability of data across multiple read operations

When opening a data node, the kernel generates the required data so it's prepared
for read operation when requested to. However, in order to ensure that multiple reads
will not create a corrupted data from that data node, a read operation alone will
not inquire the kernel to refresh the data.
To keep data output being refreshed, the userland has to re-open the data node with a
new file descriptor, or to perform the `lseek` syscall on the open file descriptor to
reset the offset to 0.

## See also

-   [`mount`(2))](help://man/2/mount).
