/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/Drivers/USBDriver.h>
#include <Kernel/Bus/USB/USBInterface.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Devices/HID/USB/MouseDevice.h>

namespace Kernel::USB {

class MouseDriver final : public Driver {
public:
    MouseDriver()
        : Driver("USB Mouse"sv)
    {
    }

    static void init();

    virtual ~MouseDriver() override = default;

    virtual ErrorOr<void> probe(USB::Device&) override;
    virtual void detach(USB::Device&) override;

private:
    USBMouseDevice::List m_interfaces;

    ErrorOr<void> checkout_interface(USB::Device&, USBInterface const&);

    ErrorOr<void> initialize_device(USB::Device&, USBInterface const&);
};

}
