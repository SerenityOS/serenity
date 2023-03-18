/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class PCIIDELegacyModeController final : public IDEController
    , public PCI::Device {
public:
    static ErrorOr<NonnullRefPtr<PCIIDELegacyModeController>> initialize(PCI::DeviceIdentifier const&, bool force_pio);

    virtual StringView device_name() const override { return "PCIIDELegacyModeController"sv; }

    bool is_bus_master_capable() const;
    bool is_pci_native_mode_enabled() const;

private:
    bool is_pci_native_mode_enabled_on_primary_channel() const;
    bool is_pci_native_mode_enabled_on_secondary_channel() const;
    explicit PCIIDELegacyModeController(PCI::DeviceIdentifier const&);

    ErrorOr<void> initialize_and_enumerate_channels(bool force_pio);

    // FIXME: Find a better way to get the ProgrammingInterface
    PCI::ProgrammingInterface m_prog_if;
    PCI::InterruptLine m_interrupt_line;
};
}
