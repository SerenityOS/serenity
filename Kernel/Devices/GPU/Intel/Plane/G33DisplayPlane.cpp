/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/GPU/Intel/Plane/G33DisplayPlane.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

ErrorOr<NonnullOwnPtr<IntelG33DisplayPlane>> IntelG33DisplayPlane::create_with_physical_address(PhysicalAddress plane_registers_start_address)
{
    auto registers_mapping = TRY(Memory::map_typed<PlaneRegisters volatile>(plane_registers_start_address, sizeof(PlaneRegisters), Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) IntelG33DisplayPlane(move(registers_mapping)));
}

IntelG33DisplayPlane::IntelG33DisplayPlane(Memory::TypedMapping<PlaneRegisters volatile> registers_mapping)
    : IntelDisplayPlane(move(registers_mapping))
{
}

ErrorOr<void> IntelG33DisplayPlane::enable(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(((m_horizontal_active_pixels_count * 4) % 64 == 0));
    VERIFY(((m_horizontal_stride) % 64 == 0));

    u32 control_value = 0;

    switch (m_pipe_select) {
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
    // Note: Bit 31 is set to turn on the plane.
    control_value |= (0b0110 << 26) | (1 << 31);

    m_plane_registers->stride = m_horizontal_stride;
    m_shadow_registers.stride = m_horizontal_stride;
    m_plane_registers->linear_offset = 0;
    m_shadow_registers.linear_offset = 0;
    m_plane_registers->surface_base = m_aperture_start.get();
    m_shadow_registers.surface_base = m_aperture_start.get();
    m_plane_registers->control = control_value;
    m_shadow_registers.control = control_value;
    return {};
}
}
