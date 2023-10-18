/*
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Driver.h>

namespace Kernel::USB {

#define USB_DEVICE_DRIVER(driver_name) REGISTER_DRIVER(driver_name)

class Device;
struct USBDeviceDescriptor;
class USBInterface;

class Driver : public Kernel::Driver {
public:
    Driver(StringView name)
        : Kernel::Driver(name)
    {
    }

    virtual ~Driver() = default;

    virtual ErrorOr<void> probe(USB::Device&) = 0;
    virtual void detach(USB::Device&) = 0;
    StringView name() const { return m_driver_name; }
};

}
