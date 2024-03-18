/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/GPUDevice.h>
#include <Kernel/Library/Driver.h>

namespace Kernel {

class VoodooGraphicsAdapter final : public GPUDevice {
    KERNEL_MAKE_DRIVER_LISTABLE(VoodooGraphicsAdapter)

public:
    static ErrorOr<NonnullRefPtr<VoodooGraphicsAdapter>> create(PCI::Device&);
    virtual ~VoodooGraphicsAdapter() = default;

private:
    ErrorOr<void> initialize_adapter();

    explicit VoodooGraphicsAdapter(PCI::Device&);

    LockRefPtr<DisplayConnector> m_display_connector;

    NonnullRefPtr<PCI::Device> const m_pci_device;
};
}
