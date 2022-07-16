/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/PCIGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeDisplayConnector;
class IntelNativeGraphicsAdapter final : public PCIGraphicsAdapter {

public:
    static RefPtr<IntelNativeGraphicsAdapter> create_instance(PCI::DeviceIdentifier const&);

    virtual ~IntelNativeGraphicsAdapter() = default;

private:
    virtual ErrorOr<void> initialize_after_sysfs_directory_creation() override;

    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGraphicsAdapter(PCI::DeviceIdentifier const&);

    RefPtr<IntelNativeDisplayConnector> m_display_connector;
};
}
