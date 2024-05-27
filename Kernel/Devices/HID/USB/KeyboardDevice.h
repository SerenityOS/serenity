/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2024, Olekoop <mlglol360xd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Devices/TTY/ConsoleManagement.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

class USBKeyboardDevice final : public KeyboardDevice {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullRefPtr<USBKeyboardDevice>> try_create_instance(USB::Device const&, size_t max_packet_size, NonnullOwnPtr<USB::InterruptInPipe> pipe);
    virtual ~USBKeyboardDevice() override {};

    USB::Device const& device() const { return *m_attached_usb_device; }

private:
    ErrorOr<void> create_and_start_polling_process(size_t max_packet_size);

    USBKeyboardDevice(USB::Device const& usb_device, NonnullOwnPtr<USB::InterruptInPipe> pipe);
    NonnullOwnPtr<USB::InterruptInPipe> m_interrupt_in_pipe;
    NonnullRefPtr<USB::Device> m_attached_usb_device;

    IntrusiveListNode<USBKeyboardDevice, NonnullRefPtr<USBKeyboardDevice>> m_list_node;
    bool m_key_pressed[6] = { false, false, false, false, false, false };
    KeyEvent m_last_event = {
        .key = Key_Invalid,
        .map_entry_index = 0xFF
    };
    u8 m_last_num_pressed_keys = 0;

public:
    using List = IntrusiveList<&USBKeyboardDevice::m_list_node>;
};

}
