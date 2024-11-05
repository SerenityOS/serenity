# Containers

## What is a container?

Container is a conceptual establishment in the SerenityOS ecosystem that
allows users to isolate user programs from each other based on exposed
unshared resources such as PIDs, filesystem view and hostname.

## Containers on the kernel side

The kernel currently exposes 3 types of possible isolation mechanisms:

-   VFS Root contexts
-   Process lists
-   Hostname contexts

### VFS Root Contexts

A VFS (virtual file system) root context is the context that each process
holds to be able to view a filesystem tree.
A VFS root context holds the mount table for its context as well as a
Custody for the root directory of the context.

User processes can hold a context that is shared for all default programs
or have a special context to restrict its filesystem view.

VFS Root Contexts are attached to a global list and are removed from that
list when their last mount (root mount) is unmounted.

### Process lists

A process list is either a global process list (which all processes are
attached to) or scoped process list.

User processes can hold a reference to a scoped process list. When this
happens, that process can only see other processes which are on the same
list.

Scoped process lists are attached to a global list and are removed from that global list
when the last process that is still attached to the list detaches from it.

### Hostname contexts

A hostname context is a mechanism that allows us a set of user processes
to share a defined hostname.

Each group of processes that hold a reference to an hostname context can
change the hostname and that change will be reflected to other processes
that are attached to the context.

Hostname contexts are attached to a global list and are removed from that
global list when the last process that is still attached to the context detaches
from it.

## Kernel-Userspace interfaces

There are 2 main syscalls to handle resource isolation:

-   `unshare_create` which creates a new isolation mechanism and returns
    an index number for a specified isolation type.
-   `unshare_attach` which attach the user process based on the index number
    and isolation type.

## Jails as a security mechanism

When the user process is jailed **until exit**, it can't create or attach to other resources.
This makes jails as an effective mechanism to create secure (sandboxed) containers,
so a user program and its descendants will always use the same resources that
were chosen upon the creation of the container.
