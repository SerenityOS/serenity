/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Devices/Storage/SD/PCISDHostController.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PCISDHostController>> PCISDHostController::try_initialize(PCI::DeviceIdentifier const& device_identifier)
{
    auto slot_information_register = read_slot_information(device_identifier);

    if (slot_information_register.slots_available() != 1) {
        // TODO: Support multiple slots
        dmesgln("SD Host Controller has {} slots, but we currently only support using only one", slot_information_register.slots_available());
    }

    auto bar = static_cast<PCI::HeaderType0BaseRegister>(slot_information_register.first_bar_number);
    auto registers = TRY(PCI::map_bar<SD::HostControlRegisterMap volatile>(device_identifier, bar));

    auto sdhc = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PCISDHostController(device_identifier, move(registers))));
    TRY(sdhc->initialize());

    PCI::enable_bus_mastering(sdhc->device_identifier());
    PCI::enable_memory_space(sdhc->device_identifier());
    sdhc->try_enable_dma();

    return sdhc;
}

PCISDHostController::PCISDHostController(PCI::DeviceIdentifier const& device_identifier, Memory::TypedMapping<SD::HostControlRegisterMap volatile> registers)
    : PCI::Device(device_identifier)
    , m_registers(move(registers))
{
}

}
