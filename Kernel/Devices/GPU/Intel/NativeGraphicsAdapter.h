/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Definitions.h>
#include <Kernel/Devices/GPU/Intel/DisplayConnectorGroup.h>
#include <Kernel/Devices/GPU/Intel/NativeDisplayConnector.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGraphicsAdapter final
    : public GPUDevice {

    KERNEL_MAKE_DRIVER_LISTABLE(IntelNativeGraphicsAdapter)
public:
    static ErrorOr<NonnullRefPtr<IntelNativeGraphicsAdapter>> create(PCI::Device&);

    virtual ~IntelNativeGraphicsAdapter() = default;

private:
    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGraphicsAdapter(PCI::Device&);

    LockRefPtr<IntelDisplayConnectorGroup> m_connector_group;
    NonnullRefPtr<PCI::Device> const m_pci_device;
};
}
