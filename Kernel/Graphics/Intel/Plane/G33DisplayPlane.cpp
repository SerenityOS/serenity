/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Intel/Plane/G33DisplayPlane.h>
#include <Kernel/PhysicalAddress.h>

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

ErrorOr<void> IntelG33DisplayPlane::set_plane_settings(Badge<IntelDisplayConnectorGroup>, PhysicalAddress aperture_start, PipeSelect pipe_select, size_t horizontal_active_pixels_count)
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
