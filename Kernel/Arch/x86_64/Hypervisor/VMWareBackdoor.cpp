/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Singleton.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Arch/x86_64/Hypervisor/VMWareBackdoor.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define VMWARE_CMD_GETVERSION 0x0a

#define VMMOUSE_READ_ID 0x45414552
#define VMMOUSE_DISABLE 0x000000f5
#define VMMOUSE_REQUEST_RELATIVE 0x4c455252
#define VMMOUSE_REQUEST_ABSOLUTE 0x53424152

#define VMMOUSE_QEMU_VERSION 0x3442554a
#define VMMOUSE_LEFT_CLICK 0x20
#define VMMOUSE_RIGHT_CLICK 0x10
#define VMMOUSE_MIDDLE_CLICK 0x08

#define VMWARE_MAGIC 0x564D5868
#define VMWARE_PORT 0x5658
#define VMWARE_PORT_HIGHBANDWIDTH 0x5659

inline void vmware_out(VMWareCommand& command)
{
    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT;
    command.si = 0;
    command.di = 0;
    asm volatile("in %%dx, %0"
                 : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

inline void vmware_high_bandwidth_send(VMWareCommand& command)
{
    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT_HIGHBANDWIDTH;

    asm volatile("cld; rep; outsb"
                 : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

inline void vmware_high_bandwidth_get(VMWareCommand& command)
{
    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT_HIGHBANDWIDTH;
    asm volatile("cld; rep; insb"
                 : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

class VMWareBackdoorDetector {
public:
    VMWareBackdoorDetector()
    {
        if (detect_presence())
            m_backdoor = adopt_nonnull_own_or_enomem(new (nothrow) VMWareBackdoor()).release_value_but_fixme_should_propagate_errors();
    }

    VMWareBackdoor* get_instance()
    {
        return m_backdoor.ptr();
    }

private:
    static bool detect_presence()
    {
        VMWareCommand command;
        command.bx = ~VMWARE_MAGIC;
        command.command = VMWARE_CMD_GETVERSION;
        vmware_out(command);
        if (command.bx != VMWARE_MAGIC || command.ax == 0xFFFFFFFF)
            return false;
        return true;
    }

    OwnPtr<VMWareBackdoor> m_backdoor;
};

static Singleton<VMWareBackdoorDetector> s_vmware_backdoor;

VMWareBackdoor* VMWareBackdoor::the()
{
    return s_vmware_backdoor->get_instance();
}

UNMAP_AFTER_INIT VMWareBackdoor::VMWareBackdoor()
{
    if (kernel_command_line().is_vmmouse_enabled())
        enable_absolute_vmmouse();
}

bool VMWareBackdoor::detect_vmmouse()
{
    VMWareCommand command;
    command.bx = VMMOUSE_READ_ID;
    command.command = VMMOUSE_COMMAND;
    send(command);
    command.bx = 1;
    command.command = VMMOUSE_DATA;
    send(command);
    if (command.ax != VMMOUSE_QEMU_VERSION)
        return false;
    return true;
}
bool VMWareBackdoor::vmmouse_is_absolute() const
{
    return m_vmmouse_absolute;
}

void VMWareBackdoor::enable_absolute_vmmouse()
{
    InterruptDisabler disabler;
    if (!detect_vmmouse())
        return;
    dmesgln("VMWareBackdoor: Enabling absolute mouse mode");

    VMWareCommand command;

    command.bx = 0;
    command.command = VMMOUSE_STATUS;
    send(command);
    if (command.ax == 0xFFFF0000) {
        dmesgln("VMWareBackdoor: VMMOUSE_STATUS got bad status");
        return;
    }

    // Enable absolute vmmouse
    command.bx = VMMOUSE_REQUEST_ABSOLUTE;
    command.command = VMMOUSE_COMMAND;
    send(command);
    m_vmmouse_absolute = true;
}
void VMWareBackdoor::disable_absolute_vmmouse()
{
    InterruptDisabler disabler;
    VMWareCommand command;
    command.bx = VMMOUSE_REQUEST_RELATIVE;
    command.command = VMMOUSE_COMMAND;
    send(command);
    m_vmmouse_absolute = false;
}

void VMWareBackdoor::send_high_bandwidth(VMWareCommand& command)
{
    vmware_high_bandwidth_send(command);

    dbgln_if(VMWARE_BACKDOOR_DEBUG, "VMWareBackdoor Command High bandwidth Send Results: EAX {:#x} EBX {:#x} ECX {:#x} EDX {:#x}",
        command.ax,
        command.bx,
        command.cx,
        command.dx);
}

void VMWareBackdoor::get_high_bandwidth(VMWareCommand& command)
{
    vmware_high_bandwidth_get(command);

    dbgln_if(VMWARE_BACKDOOR_DEBUG, "VMWareBackdoor Command High bandwidth Get Results: EAX {:#x} EBX {:#x} ECX {:#x} EDX {:#x}",
        command.ax,
        command.bx,
        command.cx,
        command.dx);
}

void VMWareBackdoor::send(VMWareCommand& command)
{
    vmware_out(command);

    dbgln_if(VMWARE_BACKDOOR_DEBUG, "VMWareBackdoor Command Send Results: EAX {:#x} EBX {:#x} ECX {:#x} EDX {:#x}",
        command.ax,
        command.bx,
        command.cx,
        command.dx);
}

u16 VMWareBackdoor::read_mouse_status_queue_size()
{
    VMWareCommand command;
    command.bx = 0;
    command.command = VMMOUSE_STATUS;
    send(command);

    if (command.ax == 0xFFFF0000) {
        dbgln_if(PS2MOUSE_DEBUG, "PS2MouseDevice: Resetting VMWare mouse");
        disable_absolute_vmmouse();
        enable_absolute_vmmouse();
        return 0;
    }

    return command.ax & 0xFFFF;
}

MousePacket VMWareBackdoor::receive_mouse_packet()
{
    VMWareCommand command;
    command.size = 4;
    command.command = VMMOUSE_DATA;
    send(command);

    int buttons = (command.ax & 0xFFFF);
    int x = command.bx;
    int y = command.cx;
    int z = static_cast<i8>(command.dx); // signed 8 bit value only!
    int w = 0;

    // horizontal scroll is reported as +-2 by qemu
    // FIXME: Scroll only functions correctly when the sign is flipped there
    if (z == 2) {
        w = -1;
        z = 0;
    } else if (z == -2) {
        w = 1;
        z = 0;
    }

    if constexpr (PS2MOUSE_DEBUG) {
        dbgln("Absolute Mouse: Buttons {:x}", buttons);
        dbgln("Mouse: x={}, y={}, z={}, w={}", x, y, z, w);
    }

    MousePacket packet;
    packet.x = x;
    packet.y = y;
    packet.z = z;
    packet.w = w;
    if (buttons & VMMOUSE_LEFT_CLICK)
        packet.buttons |= MousePacket::LeftButton;
    if (buttons & VMMOUSE_RIGHT_CLICK)
        packet.buttons |= MousePacket::RightButton;
    if (buttons & VMMOUSE_MIDDLE_CLICK)
        packet.buttons |= MousePacket::MiddleButton;

    packet.is_relative = false;
    return packet;
}

}
