## Name

proc - SerenityOS ProcFS

## Description

The kernel can expose process related information in /proc.
This functionality is used by various userland programs.
Most of the output layout in the ProcFS nodes is JSON.

### Global entries

* **`all`** - this node exports a list of all processes that currently exist.
* **`cmdline`** - this node exports the kernel boot commandline that was passed to
from the bootloader.
* **`cpuinfo`** - this node exports information on the CPU.
* **`devices`** - this node exports information on all devices that might be represented
by a device file.
* **`df`** - this node exports information on mounted filesystems and basic statistics on
them.
* **`dmesg`** - this node exports information from the kernel log.
* **`interrupts`** - this node exports information on all IRQ handlers and basic statistics on
them. 
* **`kernel_base`** - this node reveals the loading address of the kernel.
* **`keymap`** - this node exports information on current used keymap.
* **`memstat`** - this node exports statistics on memory allocation in the kernel.
* **`pci`** - this node exports information on all currently-discovered PCI devices in the system.

### `net` directory

* **`adapters`** - this node exports information on all currently-discovered network adapters.
* **`arp`** - this node exports information on the kernel ARP table.
* **`local`** - this node exports information on local (unix) sockets.
* **`tcp`** - this node exports information on tcp sockets.
* **`udp`** - this node exports information on udp sockets.

### `sys` directory

This subdirectoy includes global settings of the kernel.

* **`caps_lock_to_ctrl`** - this node controls remapping of of caps lock to the Ctrl key.
* **`kmalloc_stacks`** - this node controls whether to send information about kmalloc to debug log.
* **`ubsan_is_deadly`** - this node controls the deadliness of the kernel undefinied behavior
sanitizer errors.

### Per process entries

* **`cwd`** - a symbolic link to current work directory of a process.
* **`exe`** - a symbolic link to the executable binary of the process.
* **`fds`** - this node exports information on all currently open file descriptors.
* **`fd`** - this directory lists all currently open file descriptors.
* **`perf_events`** - this node exports information being gathered during a profile on a process.
* **`pledge`** - this node exports information on all the pledge requests and promises of a process.
* **`stacks`** - this directory lists all stack traces of process threads.
* **`unveil`** - this node exports information on all the unveil requests of a process.
* **`vm`** - this node exports information on virtual memory mappings of a process.

### Consistency and stability of data across multiple read operations

When opening a data node, the kernel generates the required data so it's prepared
for read operation when requested to. However, in order to ensure that multiple reads
will not create a corrupted data from that data node, a read operation alone will
not inquire the kernel to refresh the data.
To keep data output being refreshed, the userland has to re-open the data node with a 
new file descriptor, or to perform the `lseek` syscall on the open file descriptor to
reset the the offset to 0.

## See also

* [`mount`(2))](../man2/mount.md).
* [`boot_parameters`(7))](boot_parameters.md).
* [`pledge`(2))](../man2/pledge.md).
* [`unveil`(2))](../man2/unveil.md).
