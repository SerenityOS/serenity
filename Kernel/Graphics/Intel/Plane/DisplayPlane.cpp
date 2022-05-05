/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Intel/Plane/DisplayPlane.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

IntelDisplayPlane::IntelDisplayPlane(Memory::TypedMapping<PlaneRegisters volatile> plane_registers_mapping)
    : m_plane_registers(move(plane_registers_mapping))
{
}

IntelDisplayPlane::ShadowRegisters IntelDisplayPlane::shadow_registers() const
{
    SpinlockLocker locker(m_access_lock);
    return m_shadow_registers;
}

ErrorOr<void> IntelDisplayPlane::enable(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    // Note: We use the shadow register so we don't have the already set
    // settings being lost.
    m_plane_registers->control = m_shadow_registers.control | (1 << 31);
    m_shadow_registers.control |= (1 << 31);
    return {};
}

bool IntelDisplayPlane::is_enabled(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    return m_shadow_registers.control & (1 << 31);
}

ErrorOr<void> IntelDisplayPlane::disable(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    // Note: We use the shadow register so we don't have the already set
    // settings being lost.
    m_shadow_registers.control &= ~(1 << 31);
    m_plane_registers->control = m_shadow_registers.control;
    return {};
}

}
