/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Intel/Plane/DisplayPlane.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelG33DisplayPlane final : public IntelDisplayPlane {
public:
    static ErrorOr<NonnullOwnPtr<IntelG33DisplayPlane>> create_with_physical_address(PhysicalAddress plane_registers_start_address);

    virtual ErrorOr<void> set_plane_settings(Badge<IntelDisplayConnectorGroup>, PhysicalAddress aperture_start, PipeSelect, size_t horizontal_active_pixels_count) override;

private:
    explicit IntelG33DisplayPlane(Memory::TypedMapping<volatile IntelDisplayPlane::PlaneRegisters> plane_registers_mapping);
};
}
