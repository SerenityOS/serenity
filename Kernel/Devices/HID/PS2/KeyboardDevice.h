/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Bus/SerialIO/Device.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class PS2KeyboardDevice final : public SerialIODevice {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> try_to_initialize(SerialIOController const&, SerialIOController::PortIndex port_index, KeyboardDevice const&);
    virtual ~PS2KeyboardDevice() override;
    ErrorOr<void> initialize();

    // ^SerialIODevice
    virtual void handle_byte_read_from_serial_input(u8 byte) override;

private:
    PS2KeyboardDevice(SerialIOController const&, SerialIOController::PortIndex port_index, KeyboardDevice const&);

    bool m_has_e0_prefix { false };

    NonnullRefPtr<KeyboardDevice> const m_keyboard_device;
};

}
