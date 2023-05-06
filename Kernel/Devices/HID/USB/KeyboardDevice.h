/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/CircularQueue.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/API/MousePacket.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/KString.h>
#include <Kernel/Random.h>

namespace Kernel {

class USBKeyboardDevice final : public KeyboardDevice {
public:
    friend class DeviceManagement;
    static ErrorOr<NonnullRefPtr<USBKeyboardDevice>> try_create_instance(USB::Device const&);
    virtual ~USBKeyboardDevice() override {};

private:
    ErrorOr<void> create_and_start_polling_process();

    u8 m_previous_key { 0 };
    explicit USBKeyboardDevice(USB::Device const&);
    NonnullRefPtr<USB::Device> m_attached_usb_device;
};

}
