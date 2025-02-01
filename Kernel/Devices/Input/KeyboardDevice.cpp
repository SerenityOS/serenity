/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Input/KeyboardDevice.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

void KeyboardDevice::handle_input_event(KeyEvent queued_event)
{
    if (queued_event.key == Key_NumLock && queued_event.is_press())
        m_num_lock_on = !m_num_lock_on;

    queued_event.flags |= m_modifiers;

    if (queued_event.is_press() && (m_modifiers == (Mod_Alt | Mod_Shift) || m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift)) && queued_event.key == Key_F12) {
        // Alt+Shift+F12 pressed, dump some kernel state to the debug console.
        VirtualConsole::switch_to_debug_console();
        Scheduler::dump_scheduler_state(m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift));
    }

    {
        auto key = queued_event.key;
        if (queued_event.is_press() && (m_modifiers & Mod_Alt) != 0 && key >= Key_1 && key < Key_1 + VirtualConsole::s_max_virtual_consoles) {
            // FIXME: Do something sanely here if we can't allocate a work queue?
            MUST(g_io_work->try_queue([key]() {
                VirtualConsole::switch_to(key - Key_1);
            }));
        }
    }

    if (!g_caps_lock_remapped_to_ctrl && queued_event.key == Key_CapsLock && queued_event.is_press())
        m_caps_lock_on = !m_caps_lock_on;

    queued_event.caps_lock_on = m_caps_lock_on;

    if (g_caps_lock_remapped_to_ctrl && queued_event.key == Key_CapsLock) {
        m_caps_lock_to_ctrl_pressed = queued_event.is_press();
        update_modifier(Mod_Ctrl, m_caps_lock_to_ctrl_pressed);
    }

    if (queued_event.map_entry_index != 0xFF)
        queued_event.code_point = InputManagement::the().get_char_from_character_map(queued_event, queued_event.map_entry_index);

    // If using a non-QWERTY layout, queued_event.key needs to be updated to be the same as event.code_point
    KeyCode mapped_key = code_point_to_key_code(queued_event.code_point);
    if (mapped_key != KeyCode::Key_Invalid)
        queued_event.key = mapped_key;

    {
        SpinlockLocker locker(InputManagement::the().m_client_lock);
        if (InputManagement::the().m_client)
            InputManagement::the().m_client->on_key_pressed(queued_event);
    }

    {
        SpinlockLocker lock(m_queue_lock);
        m_queue.enqueue(queued_event);
    }

    evaluate_block_conditions();
}

ErrorOr<NonnullRefPtr<KeyboardDevice>> KeyboardDevice::try_to_initialize()
{
    return TRY(Device::try_create_device<KeyboardDevice>());
}

KeyboardDevice::KeyboardDevice()
    : InputDevice(MajorAllocation::CharacterDeviceFamily::Keyboard, InputManagement::the().generate_minor_device_number_for_keyboard())
{
}

KeyboardDevice::~KeyboardDevice() = default;

bool KeyboardDevice::can_read(OpenFileDescription const&, u64) const
{
    return !m_queue.is_empty();
}

ErrorOr<size_t> KeyboardDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    size_t nread = 0;
    SpinlockLocker lock(m_queue_lock);
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if (size - nread < sizeof(Event))
            break;
        auto event = m_queue.dequeue();

        lock.unlock();

        auto result = TRY(buffer.write_buffered<sizeof(Event)>(sizeof(Event), [&](Bytes bytes) {
            memcpy(bytes.data(), &event, sizeof(Event));
            return bytes.size();
        }));
        VERIFY(result == sizeof(Event));
        nread += sizeof(Event);

        lock.lock();
    }
    return nread;
}

ErrorOr<void> KeyboardDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case KEYBOARD_IOCTL_GET_NUM_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_num_lock_on);
    }
    case KEYBOARD_IOCTL_SET_NUM_LOCK: {
        // In this case we expect the value to be a boolean and not a pointer.
        auto num_lock_value = static_cast<u8>(arg.ptr());
        if (num_lock_value != 0 && num_lock_value != 1)
            return EINVAL;
        m_num_lock_on = !!num_lock_value;
        return {};
    }
    case KEYBOARD_IOCTL_GET_CAPS_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_caps_lock_on);
    }
    case KEYBOARD_IOCTL_SET_CAPS_LOCK: {
        auto caps_lock_value = static_cast<u8>(arg.ptr());
        if (caps_lock_value != 0 && caps_lock_value != 1)
            return EINVAL;
        m_caps_lock_on = !!caps_lock_value;
        return {};
    }
    default:
        return EINVAL;
    };
}

}
