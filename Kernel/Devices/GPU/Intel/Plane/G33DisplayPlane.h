/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Devices/GPU/Intel/Plane/DisplayPlane.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelG33DisplayPlane final : public IntelDisplayPlane {
public:
    static ErrorOr<NonnullOwnPtr<IntelG33DisplayPlane>> create_with_physical_address(PhysicalAddress plane_registers_start_address);

    virtual ErrorOr<void> enable(Badge<IntelDisplayConnectorGroup>) override;

private:
    explicit IntelG33DisplayPlane(Memory::TypedMapping<IntelDisplayPlane::PlaneRegisters volatile> plane_registers_mapping);
};
}
