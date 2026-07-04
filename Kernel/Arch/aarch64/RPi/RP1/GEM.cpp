/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1/GEM.h>
#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Net/Cadence/GEMRegisters.h>

namespace Kernel::RPi {

ErrorOr<NonnullRefPtr<RP1GEMNetworkAdapter>> RP1GEMNetworkAdapter::create(RP1& rp1, StringView interface_name, PhysicalAddress paddr, InterruptNumber interrupt_number)
{
    auto registers_mapping = TRY(Memory::map_typed_writable<CadenceGEMNetworkAdapter::Registers volatile>(paddr));

    MACAddress mac_address;

    // Since this isn't a devicetree driver, we unfortunately have to hardcode this absolute path here
    // and hope that none of these node names will be different in future firmware updates.
    auto mac_addr_prop = DeviceTree::get().resolve_property("/axi/pcie@1000120000/rp1/ethernet@100000/local-mac-address"sv);
    if (mac_addr_prop.has_value() && mac_addr_prop->size() == 6) {
        mac_address = MACAddress { mac_addr_prop->as<Array<u8, 6>>() };
    } else {
        dmesgln("Failed to retrieve RP1 GEM MAC address from the devicetree");
        // TODO: We could fall back to a random MAC address here.
        return EINVAL;
    }

    auto adapter = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RP1GEMNetworkAdapter(rp1, interface_name, mac_address, move(registers_mapping), interrupt_number)));
    TRY(adapter->initialize());

    return adapter;
}

RP1GEMNetworkAdapter::RP1GEMNetworkAdapter(RP1& rp1, StringView interface_name, MACAddress mac_address, Memory::TypedMapping<CadenceGEMNetworkAdapter::Registers volatile> registers, InterruptNumber interrupt_number)
    : CadenceGEMNetworkAdapter(interface_name, mac_address, move(registers))
    , m_rp1(rp1)
    , m_interrupt_number(interrupt_number)
{
}

void RP1GEMNetworkAdapter::register_and_enable_interrupt()
{
    // 6.2. MSIx configuration registers
    // "The only true edge-level interrupts in RP1 are the set of vectors assigned to USBHOST0 and USBHOST1."
    // So the ethernet controller has a level-triggered interrupt.
    m_rp1.register_interrupt_handler(m_interrupt_number, RP1::InterruptTriggerMode::Level, [this](InterruptNumber) {
        return handle_interrupt();
    });
}

u32 RP1GEMNetworkAdapter::mdio_clock_input_frequency()
{
    // The MDC clock divider uses clk_sys as the input clock on the RP1. clk_sys runs at 200 MHz by default.
    // FIXME: Ideally, we should derive clock routing from the devicetree, which would require some
    //        generic infrastructure for devicetree clocks.
    return 200'000'000;
}

}
