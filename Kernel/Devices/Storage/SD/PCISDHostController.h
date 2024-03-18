/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class PCISDHostController : public SDHostController {
    KERNEL_MAKE_DRIVER_LISTABLE(PCISDHostController)
public:
    static ErrorOr<NonnullRefPtr<PCISDHostController>> try_initialize(PCI::Device&);

protected:
    // ^SDHostController
    virtual SD::HostControlRegisterMap volatile* get_register_map_base_address() override { return m_registers.ptr(); }

private:
    PCISDHostController(PCI::Device& pci_device);

    struct [[gnu::packed]] SlotInformationRegister {
        u8 first_bar_number : 3;
        u8 : 1;
        u8 number_of_slots : 3;
        u8 : 1;

        u8 slots_available() const { return number_of_slots + 1; }
    };
    static_assert(AssertSize<SlotInformationRegister, 1>());

    SlotInformationRegister read_slot_information() const;

    NonnullRefPtr<PCI::Device> const m_pci_device;
    Memory::TypedMapping<SD::HostControlRegisterMap volatile> m_registers;
};
}
