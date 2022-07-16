/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/PCIGraphicsAdapter.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class GraphicsManagement;
struct BochsDisplayMMIORegisters;

class BochsDisplayConnector;
class BochsGraphicsAdapter final : public PCIGraphicsAdapter {
    friend class GraphicsManagement;

public:
    static NonnullRefPtr<BochsGraphicsAdapter> create_instance(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;

private:
    virtual ErrorOr<void> initialize_after_sysfs_directory_creation() override;

    ErrorOr<void> initialize_adapter();

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    bool const bochs_hardware { false };
    bool const virtual_box_hardware { false };
    RefPtr<BochsDisplayConnector> m_display_connector;
};
}
