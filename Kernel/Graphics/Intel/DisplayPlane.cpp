/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Intel/DisplayPlane.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

ErrorOr<NonnullOwnPtr<IntelDisplayPlane>> IntelDisplayPlane::create_with_physical_address(PhysicalAddress plane_registers_start_address)
{
    auto registers_mapping = TRY(Memory::map_typed<PlaneRegisters volatile>(plane_registers_start_address, sizeof(PlaneRegisters), Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) IntelDisplayPlane(move(registers_mapping)));
}

IntelDisplayPlane::IntelDisplayPlane(Memory::TypedMapping<PlaneRegisters volatile> registers_mapping)
    : m_plane_registers(move(registers_mapping))
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

ErrorOr<void> IntelDisplayPlane::set_plane_settings(Badge<IntelDisplayConnectorGroup>, PhysicalAddress aperture_start, PipeSelect pipe_select, size_t horizontal_active_pixels_count)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(((horizontal_active_pixels_count * 4) % 64 == 0));
    VERIFY(aperture_start < PhysicalAddress(0x1'0000'0000));

    u32 control_value = 0;

    switch (pipe_select) {
    case PipeSelect::PipeA:
        control_value |= (0b00 << 24);
        break;
    case PipeSelect::PipeB:
        control_value |= (0b01 << 24);
        break;
    case PipeSelect::PipeC:
        control_value |= (0b10 << 24);
        break;
    case PipeSelect::PipeD:
        control_value |= (0b11 << 24);
        break;
    }

    // Note: Set the plane to work with 32 bit BGRX (Ignore Alpha channel).
    control_value |= (0b0110 << 26);

    m_plane_registers->stride = horizontal_active_pixels_count * 4;
    m_shadow_registers.stride = horizontal_active_pixels_count * 4;
    m_plane_registers->linear_offset = 0;
    m_shadow_registers.linear_offset = 0;
    m_plane_registers->surface_base = aperture_start.get();
    m_shadow_registers.surface_base = aperture_start.get();
    m_plane_registers->control = control_value;
    m_shadow_registers.control = control_value;

    return {};
}

}
