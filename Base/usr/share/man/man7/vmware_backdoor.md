## Name

VMWare backdoor - a communication channel with hypervisor

## Description

The kernel supports the VMWare backdoor communication channel. This
functionality can be used to enable features directly.

Currently the implementation only supports enabling the vmmouse features.

## Communication protocol

The VMWare backdoor is essentially a communication channel, that allows the
guest OS to enable features on the host, to read data on the host machine, etc.

To initiate a VMWare command, the code should construct a `VMWareCommand` and to
set the correct values. Then, it can send the command to hypervisor by calling
the `VMWareBackdoor::send()` with a reference of the `VMWareCommand` that was
constructed. `VMWareBackdoor::send()` is doing an x86 IO operation to a special
IO port with a magic value in EAX register.

After the hypervisor has been processing the `VMWareCommand`, it returns the
results (if there are any) to guest OS through the EAX, EBX, ECX and EDX
registers.

## VMMouse feature

### Description

The VMMouse feature is an expansion to the PS/2 mouse protocol. On emulators
that don't support VMMouse, the emulator has to capture/release the mouse cursor
on user request. VMMouse enables automatic capture/release of the mouse cursor.
QEMU supports the VMWare backdoor, and also supports the VMMouse feature.

### Enabling

To enable we should send commands in this sequence

### Disabling

## Examples

### `VMWareCommand` setting up

```c++
void vmware_cmd(u32 bx, u32 command)
{
    VMWareCommand command;
    command.bx = bx ;
    command.command = command;
    send(command);
}

```

### `VMWareCommand` setting up with size argument

```c++
void vmware_cmd(u32 size, u32 command)
{
    VMWareCommand command;
    command.size = size;
    command.command = command;
    send(command);
}
```
