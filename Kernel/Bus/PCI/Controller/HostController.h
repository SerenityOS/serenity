/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

TYPEDEF_DISTINCT_ORDERED_ID(u8, BusNumber);
TYPEDEF_DISTINCT_ORDERED_ID(u8, DeviceNumber);
TYPEDEF_DISTINCT_ORDERED_ID(u8, FunctionNumber);

class HostController {
public:
    virtual ~HostController() = default;

    virtual void write8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) = 0;
    virtual void write16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) = 0;
    virtual void write32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) = 0;

    virtual u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u32 read32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;

    u32 domain_number() const { return m_domain.domain_number(); }

    virtual void enumerate_attached_devices(Function<void(DeviceIdentifier)> callback) = 0;

protected:
    explicit HostController(PCI::Domain const& domain)
        : m_domain(domain)
    {
    }

    const PCI::Domain m_domain;
};

}
