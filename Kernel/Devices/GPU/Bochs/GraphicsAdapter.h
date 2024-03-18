/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Bochs/Definitions.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/GPUDevice.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class GraphicsManagement;
struct BochsDisplayMMIORegisters;

class BochsGraphicsAdapter final : public GPUDevice {
    friend class GraphicsManagement;

    KERNEL_MAKE_DRIVER_LISTABLE(BochsGraphicsAdapter)
public:
    static ErrorOr<NonnullRefPtr<BochsGraphicsAdapter>> create(PCI::Device const&);
    virtual ~BochsGraphicsAdapter() = default;

private:
    ErrorOr<void> initialize_adapter();

    explicit BochsGraphicsAdapter(PCI::Device const&);

    LockRefPtr<DisplayConnector> m_display_connector;
    NonnullRefPtr<PCI::Device> const m_pci_device;
};
}
