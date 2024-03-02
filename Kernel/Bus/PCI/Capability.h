/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

class Device;
class Capability {
public:
    Capability(PCI::Device& device, u8 id, u8 ptr)
        : m_device(device)
        , m_id(id)
        , m_ptr(ptr)
    {
    }

    CapabilityID id() const { return m_id; }

    u8 read8(size_t offset);
    u16 read16(size_t offset);
    u32 read32(size_t offset);
    void write8(size_t offset, u8 value);
    void write16(size_t offset, u16 value);
    void write32(size_t offset, u32 value);

    u8 ptr() const { return m_ptr; }

private:
    Device& m_device;
    const CapabilityID m_id;
    const u8 m_ptr;
};

}
