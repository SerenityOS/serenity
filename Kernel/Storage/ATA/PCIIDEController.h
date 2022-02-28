/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/ATA/IDEChannel.h>
#include <Kernel/Storage/ATA/IDEController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class PCIIDEController final : public IDEController
    , public PCI::Device {
public:
    static NonnullRefPtr<PCIIDEController> initialize(PCI::DeviceIdentifier const&, bool force_pio);

    bool is_bus_master_capable() const;
    bool is_pci_native_mode_enabled() const;

private:
    bool is_pci_native_mode_enabled_on_primary_channel() const;
    bool is_pci_native_mode_enabled_on_secondary_channel() const;
    PCIIDEController(PCI::DeviceIdentifier const&, bool force_pio);

    RefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    void initialize(bool force_pio);
    void detect_disks();

    // FIXME: Find a better way to get the ProgrammingInterface
    PCI::ProgrammingInterface m_prog_if;
    PCI::InterruptLine m_interrupt_line;
};
}
