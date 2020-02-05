/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/VMWareBackdoor.h>
#include <Kernel/IO.h>

#define VMWARE_CMD_GETVERSION 0x0a

#define VMMOUSE_READ_ID 0x45414552
#define VMMOUSE_DISABLE 0x000000f5
#define VMMOUSE_REQUEST_RELATIVE 0x4c455252
#define VMMOUSE_REQUEST_ABSOLUTE 0x53424152

static VMWareBackdoor* s_vmware_backdoor;

static bool is_initialized()
{
    return s_vmware_backdoor != nullptr;
}

void VMWareBackdoor::initialize()
{
    if (!is_initialized())
        s_vmware_backdoor = new VMWareBackdoor;
}

VMWareBackdoor& VMWareBackdoor::the()
{
    ASSERT(s_vmware_backdoor != nullptr);
    return *s_vmware_backdoor;
}

VMWareBackdoor::VMWareBackdoor()
{
    if (!detect_presence()) {
        kprintf("VMWare Backdoor: Not supported!\n");
        m_supported = false;
        return;
    }
    kprintf("VMWare Backdoor: Supported.\n");
    m_supported = true;
}
bool VMWareBackdoor::detect_presence()
{
    VMWareCommand command;
    command.bx = ~VMWARE_MAGIC;
    command.command = VMWARE_CMD_GETVERSION;
    IO::vmware_out(command);
    if (command.bx != VMWARE_MAGIC || command.ax == 0xFFFFFFFF)
        return false;
    return true;
}

bool VMWareBackdoor::supported()
{
    return m_supported;
}

bool VMWareBackdoor::detect_vmmouse()
{
    if (!supported())
        return false;
    VMWareCommand command;
    command.bx = 0;
    command.command = VMMOUSE_GETVERSION;
    send(command);
    // Apparently after a VMMOUSE_GETVERSION command, we should see that EBX register = VMWARE_MAGIC
    if (command.bx != VMWARE_MAGIC || command.ax == 0xFFFFFFFF)
        return false;
    return true;
}
bool VMWareBackdoor::vmmouse_is_absolute()
{
    return m_vmmouse_absolute;
}

void VMWareBackdoor::enable_absolute_vmmouse()
{
    InterruptDisabler disabler;
    if (!supported() || !detect_vmmouse())
        return;
    dbgprintf("Enabling vmmouse, absolute mode\n");

    VMWareCommand command;

    // Enable vmmouse
    command.bx = VMMOUSE_READ_ID;
    command.command = VMMOUSE_COMMAND;
    send(command);

    // Status
    command.bx = 0;
    command.command = VMMOUSE_STATUS;

    send(command);

    // Read Data
    command.bx = 1;
    command.command = VMMOUSE_DATA;
    send(command);

    // Enable absolute vmmouse
    command.bx = VMMOUSE_REQUEST_ABSOLUTE;
    command.command = VMMOUSE_COMMAND;
    send(command);
    m_vmmouse_absolute = true;
}
void VMWareBackdoor::disable_absolute_vmmouse()
{
    InterruptDisabler disabler;
    if (!supported())
        return;
    VMWareCommand command;
    command.bx = VMMOUSE_REQUEST_RELATIVE;
    command.command = VMMOUSE_COMMAND;
    send(command);
    m_vmmouse_absolute = false;
}

void VMWareBackdoor::send_highbandwidth(VMWareCommand& command)
{
    if (supported())
        IO::vmware_highbandwidth_send(command);
}

void VMWareBackdoor::get_highbandwidth(VMWareCommand& command)
{
    if (supported())
        IO::vmware_highbandwidth_get(command);
}

void VMWareBackdoor::send(VMWareCommand& command)
{
    if (supported())
        IO::vmware_out(command);
}
