/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/Intel/Definitions.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelDisplayPlane {
public:
    enum class PipeSelect {
        PipeA,
        PipeB,
        PipeC,
        PipeD,
    };

    // Note: This is used to "cache" all the registers we wrote to, because
    // we might not be able to read them directly from hardware later.
    struct ShadowRegisters {
        u32 control;
        u32 linear_offset;
        u32 stride;
        u32 surface_base;
    };

public:
    static ErrorOr<NonnullOwnPtr<IntelDisplayPlane>> create_with_physical_address(PhysicalAddress plane_registers_start_address);

    ErrorOr<void> set_plane_settings(Badge<IntelDisplayConnectorGroup>, PhysicalAddress aperture_start, PipeSelect, size_t horizontal_active_pixels_count);
    ErrorOr<void> enable(Badge<IntelDisplayConnectorGroup>);
    bool is_enabled(Badge<IntelDisplayConnectorGroup>);
    ErrorOr<void> disable(Badge<IntelDisplayConnectorGroup>);

    ShadowRegisters shadow_registers() const;

private:
    struct [[gnu::packed]] PlaneRegisters {
        u32 control;
        u32 linear_offset;
        u32 stride;
        u8 padding[24]; // Note: This might contain other registers, don't touch them.
        u32 surface_base;
    };

    explicit IntelDisplayPlane(Memory::TypedMapping<PlaneRegisters volatile> registers_mapping);
    mutable Spinlock<LockRank::None> m_access_lock;
    ShadowRegisters m_shadow_registers {};
    Memory::TypedMapping<PlaneRegisters volatile> m_plane_registers;
};
}
