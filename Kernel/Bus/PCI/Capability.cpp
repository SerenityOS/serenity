/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Capability.h>
#include <Kernel/Bus/PCI/Device.h>

namespace Kernel::PCI {

u8 Capability::read8(size_t offset)
{
    return m_device.config_space_read8(static_cast<PCI::RegisterOffset>(m_ptr + offset));
}

u16 Capability::read16(size_t offset)
{
    return m_device.config_space_read16(static_cast<PCI::RegisterOffset>(m_ptr + offset));
}

u32 Capability::read32(size_t offset)
{
    return m_device.config_space_read32(static_cast<PCI::RegisterOffset>(m_ptr + offset));
}

void Capability::write8(size_t offset, u8 value)
{
    m_device.config_space_write8(static_cast<PCI::RegisterOffset>(m_ptr + offset), value);
}

void Capability::write16(size_t offset, u16 value)
{
    m_device.config_space_write16(static_cast<PCI::RegisterOffset>(m_ptr + offset), value);
}

void Capability::write32(size_t offset, u32 value)
{
    m_device.config_space_write32(static_cast<PCI::RegisterOffset>(m_ptr + offset), value);
}

}
