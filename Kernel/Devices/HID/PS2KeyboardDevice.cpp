/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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
#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/HID/PS2KeyboardDevice.h>
#include <Kernel/IO.h>
#include <Kernel/TTY/VirtualConsole.h>

namespace Kernel {

#define IRQ_KEYBOARD 1

void PS2KeyboardDevice::irq_handle_byte_read(u8 byte)
{
    u8 ch = byte & 0x7f;
    bool pressed = !(byte & 0x80);

    m_entropy_source.add_random_event(byte);

    if (byte == 0xe0) {
        m_has_e0_prefix = true;
        return;
    }

    dbgln_if(KEYBOARD_DEBUG, "Keyboard::irq_handle_byte_read: {:#02x} {}", ch, (pressed ? "down" : "up"));
    switch (ch) {
    case 0x38:
        if (m_has_e0_prefix)
            update_modifier(Mod_AltGr, pressed);
        else
            update_modifier(Mod_Alt, pressed);
        break;
    case 0x1d:
        update_modifier(Mod_Ctrl, pressed);
        break;
    case 0x5b:
        update_modifier(Mod_Super, pressed);
        break;
    case 0x2a:
    case 0x36:
        update_modifier(Mod_Shift, pressed);
        break;
    }
    switch (ch) {
    case I8042_ACK:
        break;
    default:
        if (m_modifiers & Mod_Alt) {
            switch (ch) {
            case 0x02 ... 0x07: // 1 to 6
                VirtualConsole::switch_to(ch - 0x02);
                break;
            default:
                key_state_changed(ch, pressed);
                break;
            }
        } else {
            key_state_changed(ch, pressed);
        }
    }
}

void PS2KeyboardDevice::handle_irq(const RegisterState&)
{
    // The controller will read the data and call irq_handle_byte_read
    // for the appropriate device
    m_i8042_controller->irq_process_input_buffer(HIDDevice::Type::Keyboard);
}

UNMAP_AFTER_INIT RefPtr<PS2KeyboardDevice> PS2KeyboardDevice::try_to_initialize(const I8042Controller& ps2_controller)
{
    auto device = adopt(*new PS2KeyboardDevice(ps2_controller));
    if (device->initialize())
        return device;
    return nullptr;
}

UNMAP_AFTER_INIT bool PS2KeyboardDevice::initialize()
{
    if (!m_i8042_controller->reset_device(HIDDevice::Type::Keyboard)) {
        dbgln("KeyboardDevice: I8042 controller failed to reset device");
        return false;
    }
    return true;
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::PS2KeyboardDevice(const I8042Controller& ps2_controller)
    : IRQHandler(IRQ_KEYBOARD)
    , KeyboardDevice()
    , I8042Device(ps2_controller)
{
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::~PS2KeyboardDevice()
{
}

}
