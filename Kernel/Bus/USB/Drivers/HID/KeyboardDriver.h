/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2024, Olekoop <mlglol360xd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Devices/HID/USB/KeyboardDevice.h>

namespace Kernel::USB {

class KeyboardDriver final : public Driver {
public:
    KeyboardDriver()
        : Driver("USB Keyboard"sv)
    {
    }

    static void init();

    virtual ~KeyboardDriver() override = default;

    virtual ErrorOr<void> probe(USB::Device&) override;
    virtual void detach(USB::Device&) override;

private:
    USBKeyboardDevice::List m_interfaces;

    ErrorOr<void> checkout_interface(USB::Device&, USBInterface const&);

    ErrorOr<void> initialize_device(USB::Device&, USBInterface const&);
};

}
