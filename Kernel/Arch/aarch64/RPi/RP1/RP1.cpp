/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1/RP1.h>
#include <Kernel/Arch/aarch64/RPi/RP1/xHCI.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Library/Panic.h>

namespace Kernel::RPi {

ErrorOr<NonnullRefPtr<RP1>> RP1::create(PCI::DeviceIdentifier const& pci_identifier)
{
    auto rp1 = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RP1(pci_identifier)));
    TRY(rp1->initialize());
    return rp1;
}

RP1::RP1(PCI::DeviceIdentifier const& pci_indentifier)
    : PCI::Device(pci_indentifier)
    , PCI::IRQHandler(*this, 261) // FIXME: Don't hardcode the interrupt number.
                                  //        This number is >= 256, so the interrupt line number we wrote into the PCI config space is unfortunately truncated :^(
                                  //        We should probably store interrupt numbers somewhere other than the 8-bit Interrupt Line register.
{
}

ErrorOr<void> RP1::initialize()
{
    PCI::enable_memory_space(device_identifier());
    PCI::enable_bus_mastering(device_identifier());

    auto bar1_address = TRY(get_bar_address(device_identifier(), PCI::HeaderType0BaseRegister::BAR1));

    // 2.3.1. PCIe and 40-bit to peripheral address mapping
    m_pcie_ep_registers = TRY(Memory::map_typed_writable<RP1PCIeEndpointRegisters volatile>(bar1_address.offset(0x10'8000)));

    for (size_t i = 0; i < array_size(m_pcie_ep_registers->msix_configuration); i++)
        m_pcie_ep_registers->msix_configuration[i] &= ~RP1PCIeEndpointRegisters::MSIXConfiguration::Enable;

    enable_irq();
    enable_pin_based_interrupts();

    // Chapter 5. USB, https://datasheets.raspberrypi.com/rp1/rp1-peripherals.pdf
    // The interrupt numbers are taken from the devicetree.
    auto usbhost0 = TRY(RP1xHCIController::try_to_initialize(*this, bar1_address.offset(0x20'0000), 0, 31));
    auto usbhost1 = TRY(RP1xHCIController::try_to_initialize(*this, bar1_address.offset(0x30'0000), 1, 36));

    USB::USBManagement::the().add_controller(usbhost0);
    USB::USBManagement::the().add_controller(usbhost1);

    return {};
}

void RP1::register_interrupt_handler(InterruptNumber interrupt_number, InterruptTriggerMode trigger_mode, InterruptHandler handler)
{
    m_interrupt_handlers[interrupt_number.value()] = move(handler);

    // Level-triggered interrupts require special handling, see 6.2. MSIx configuration registers.
    if (trigger_mode == InterruptTriggerMode::Level) {
        m_interrupt_level_triggered |= 1uz << interrupt_number.value();
        m_pcie_ep_registers->msix_configuration[interrupt_number.value()] |= RP1PCIeEndpointRegisters::MSIXConfiguration::EnableInterruptAcknowledge;
    } else {
        m_interrupt_level_triggered &= ~(1uz << interrupt_number.value());
        m_pcie_ep_registers->msix_configuration[interrupt_number.value()] &= ~RP1PCIeEndpointRegisters::MSIXConfiguration::EnableInterruptAcknowledge;
    }

    m_pcie_ep_registers->msix_configuration[interrupt_number.value()] |= RP1PCIeEndpointRegisters::MSIXConfiguration::Enable;
}

bool RP1::handle_irq()
{
    // FIXME: Use MSI-X rather than reading INTSTATL and INSTATH.
    //        This requires supporting the BCM2712 MSI controller first.

    u64 interrupt_status = (static_cast<u64>(m_pcie_ep_registers->interrupt_status_high) << 32) | m_pcie_ep_registers->interrupt_status_low;

    bool was_handled = false;

    for (size_t interrupt_number = 0; interrupt_number < m_interrupt_handlers.size(); interrupt_number++) {
        if ((interrupt_status & 1uz << interrupt_number) == 0)
            continue;

        if (!m_interrupt_handlers[interrupt_number]) {
            // Some interrupts, like the ethernet controller interrupt, are sometimes already pending
            // when the firmware jumps to us, so we shouldn't panic on unexpected interrupts here.
            // Maybe the firmware didn't reset some RP1 peripherals properly?
            // The INTSTAT{L,H} bits don't seem to be affected by MSIX_CFG_*.ENABLE.
            // This workaround can likely be removed once we use MSI-X for the RP1.
            continue;
        }

        was_handled |= m_interrupt_handlers[interrupt_number](interrupt_number);

        // Level-triggered interrupts require special handling, see 6.2. MSIx configuration registers.
        if (m_interrupt_level_triggered & (1uz << interrupt_number))
            m_pcie_ep_registers->msix_configuration[interrupt_number] |= RP1PCIeEndpointRegisters::MSIXConfiguration::InterruptAcknowledge;
    }

    return was_handled;
}

PCI_DRIVER(RP1Driver);

ErrorOr<void> RP1Driver::probe(PCI::DeviceIdentifier const& pci_device_identifier) const
{
    if (kernel_command_line().disable_usb())
        return EPERM;

    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::RaspberryPi
        || pci_device_identifier.hardware_id().device_id != PCI::DeviceID::RaspberryPiRP1)
        return ENOTSUP;

    // The classes for the RP1 peripherals hold a reference to the RP1, so we can safely discard ours here.
    (void)TRY(RP1::create(pci_device_identifier));

    return {};
}

}
