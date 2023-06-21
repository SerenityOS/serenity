/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class PCISDHostController : public PCI::Device
    , public SDHostController {
public:
    static ErrorOr<NonnullRefPtr<PCISDHostController>> try_initialize(PCI::DeviceIdentifier const& device_identifier);

    // ^PCI::Device
    virtual StringView device_name() const override { return "SD Host Controller"sv; }

protected:
    // ^SDHostController
    virtual SD::HostControlRegisterMap volatile* get_register_map_base_address() override { return m_registers.ptr(); }

private:
    PCISDHostController(PCI::DeviceIdentifier const& device_identifier);

    struct [[gnu::packed]] SlotInformationRegister {
        u8 first_bar_number : 3;
        u8 : 1;
        u8 number_of_slots : 3;
        u8 : 1;

        u8 slots_available() const { return number_of_slots + 1; }
    };
    static_assert(AssertSize<SlotInformationRegister, 1>());

    SlotInformationRegister read_slot_information() const
    {
        SpinlockLocker locker(device_identifier().operation_lock());
        return bit_cast<SlotInformationRegister>(PCI::Access::the().read8_field(device_identifier(), 0x40));
    }

    Memory::TypedMapping<SD::HostControlRegisterMap volatile> m_registers;
};

}
