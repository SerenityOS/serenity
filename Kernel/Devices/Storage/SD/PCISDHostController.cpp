/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/SD/PCISDHostController.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PCISDHostController>> PCISDHostController::try_initialize(PCI::Device& pci_device)
{
    auto sdhc = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PCISDHostController(pci_device)));
    TRY(sdhc->initialize());

    pci_device.enable_bus_mastering();
    pci_device.enable_memory_space();
    sdhc->try_enable_dma();

    return sdhc;
}

PCISDHostController::SlotInformationRegister PCISDHostController::read_slot_information() const
{
    return bit_cast<SlotInformationRegister>(m_pci_device->config_space_read8(static_cast<PCI::RegisterOffset>(0x40)));
}

PCISDHostController::PCISDHostController(PCI::Device& pci_device)
    : SDHostController()
    , m_pci_device(pci_device)
{
    auto slot_information_register = read_slot_information();

    if (slot_information_register.slots_available() != 1) {
        // TODO: Support multiple slots
        dmesgln("SD Host Controller has {} slots, but we currently only support using only one", slot_information_register.slots_available());
    }

    auto physical_address_of_sdhc_registers = PhysicalAddress {
        m_pci_device->resources()[slot_information_register.first_bar_number].physical_memory_address()
    };
    m_registers = Memory::map_typed_writable<SD::HostControlRegisterMap volatile>(physical_address_of_sdhc_registers).release_value_but_fixme_should_propagate_errors();
}

}
