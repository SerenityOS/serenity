## Name

proc - SerenityOS ProcFS

## Description

The kernel can expose process related information in /proc.
This functionality is used by various userland programs.
All of the output layout (besides symbolic links) in the ProcFS nodes is JSON.

### Per process entries

-   **`cwd`** - a symbolic link to current work directory of a process.
-   **`exe`** - a symbolic link to the executable binary of the process.
-   **`fds`** - this node exports information on all currently open file descriptors.
-   **`fd`** - this directory lists all currently open file descriptors.
-   **`perf_events`** - this node exports information being gathered during a profile on a process.
-   **`pledge`** - this node exports information on all the pledge requests and promises of a process.
-   **`stacks`** - this directory lists all stack traces of process threads.
-   **`unveil`** - this node exports information on all the unveil requests of a process.
-   **`vm`** - this node exports information on virtual memory mappings of a process.
-   **`children`** - this directory lists all the child processes of a process.

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
-   [`boot_parameters`(7))](help://man/7/boot_parameters).
-   [`pledge`(2))](help://man/2/pledge).
-   [`unveil`(2))](help://man/2/unveil).
