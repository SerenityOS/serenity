/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Devices/HID/PS2KeyboardDevice.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/WorkQueue.h>

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

    if ((m_modifiers == (Mod_Alt | Mod_Shift) || m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift)) && byte == 0x58) {
        // Alt+Shift+F12 pressed, dump some kernel state to the debug console.
        ConsoleManagement::the().switch_to_debug();
        Scheduler::dump_scheduler_state(m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift));
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
        m_left_super_pressed = pressed;
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x5c:
        m_right_super_pressed = pressed;
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x2a:
        m_left_shift_pressed = pressed;
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x36:
        m_right_shift_pressed = pressed;
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    }
    switch (ch) {
    case I8042Response::Acknowledge:
        break;
    default:
        if ((m_modifiers & Mod_Alt) != 0 && ch >= 2 && ch <= ConsoleManagement::s_max_virtual_consoles + 1) {
            g_io_work->queue([ch]() {
                ConsoleManagement::the().switch_to(ch - 0x02);
            });
        }
        key_state_changed(ch, pressed);
    }
}

bool PS2KeyboardDevice::handle_irq(const RegisterState&)
{
    // The controller will read the data and call irq_handle_byte_read
    // for the appropriate device
    return m_i8042_controller->irq_process_input_buffer(HIDDevice::Type::Keyboard);
}

UNMAP_AFTER_INIT RefPtr<PS2KeyboardDevice> PS2KeyboardDevice::try_to_initialize(const I8042Controller& ps2_controller)
{
    auto keyboard_device_or_error = DeviceManagement::try_create_device<PS2KeyboardDevice>(ps2_controller);
    // FIXME: Find a way to propagate errors
    VERIFY(!keyboard_device_or_error.is_error());
    if (keyboard_device_or_error.value()->initialize())
        return keyboard_device_or_error.release_value();
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
