/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Library/LockRefPtr.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class PIIX4IDEController final
    : public IDEController {
    KERNEL_MAKE_DRIVER_LISTABLE(PIIX4IDEController)
public:
    static ErrorOr<NonnullRefPtr<PIIX4IDEController>> initialize(PCI::Device&, bool force_pio);

    bool is_bus_master_capable() const;
    bool is_pci_native_mode_enabled() const;

private:
    bool is_pci_native_mode_enabled_on_primary_channel() const;
    bool is_pci_native_mode_enabled_on_secondary_channel() const;
    explicit PIIX4IDEController(PCI::Device&);

    ErrorOr<void> initialize_and_enumerate_channels(bool force_pio);

    NonnullRefPtr<PCI::Device> const m_pci_device;

    // FIXME: Find a better way to get the ProgrammingInterface
    PCI::ProgrammingInterface m_prog_if;
    PCI::InterruptLine m_interrupt_line;
};
}
