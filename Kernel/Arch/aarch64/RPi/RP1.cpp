/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/RP1.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/USB/USBManagement.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Interrupts/IRQHandler.h>

namespace Kernel::RPi {

class RP1xHCIController final : public USB::xHCI::xHCIController {
public:
    static ErrorOr<NonnullLockRefPtr<RP1xHCIController>> try_to_initialize(PhysicalAddress, size_t index, size_t interrupt_number);

private:
    RP1xHCIController(Memory::TypedMapping<u8> registers_mapping, size_t index, size_t interrupt_number);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<OwnPtr<GenericInterruptHandler>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("xHCI: RP1 USBHOST{}: "sv, m_index));
        return {};
    }

    size_t m_index { 0 };
    size_t m_interrupt_number { 0 };
    bool m_using_message_signalled_interrupts { false };
};

ErrorOr<NonnullLockRefPtr<RP1xHCIController>> RP1xHCIController::try_to_initialize(PhysicalAddress paddr, size_t index, size_t interrupt_number)
{
    auto registers_mapping = TRY(Memory::map_typed_writable<u8>(paddr));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) RP1xHCIController(move(registers_mapping), index, interrupt_number)));
    TRY(controller->initialize());
    return controller;
}

UNMAP_AFTER_INIT RP1xHCIController::RP1xHCIController(Memory::TypedMapping<u8> registers_mapping, size_t index, size_t interrupt_number)
    : xHCIController(move(registers_mapping))
    , m_index(index)
    , m_interrupt_number(interrupt_number)
{
}

ErrorOr<OwnPtr<GenericInterruptHandler>> RP1xHCIController::create_interrupter(u16)
{
    // FIXME: Add interrupt support. This requires adding support for the BCM2712 MSI-X interrupt controller.
    return nullptr;
}

ErrorOr<void> RP1::try_to_initialize_xhci_controllers(PCI::DeviceIdentifier const& pci_identifier)
{
    PCI::enable_memory_space(pci_identifier);
    PCI::enable_bus_mastering(pci_identifier);

    auto bar1_address = TRY(get_bar_address(pci_identifier, PCI::HeaderType0BaseRegister::BAR1));

    // Chapter 5. USB, https://datasheets.raspberrypi.com/rp1/rp1-peripherals.pdf
    // The interrupt numbers are taken from the devicetree.
    auto usbhost0 = TRY(RP1xHCIController::try_to_initialize(bar1_address.offset(0x20'0000), 0, 31));
    auto usbhost1 = TRY(RP1xHCIController::try_to_initialize(bar1_address.offset(0x30'0000), 1, 36));

    USB::USBManagement::the().add_controller(usbhost0);
    USB::USBManagement::the().add_controller(usbhost1);

    return {};
}

PCI_DRIVER(RP1Driver);

ErrorOr<void> RP1Driver::probe(PCI::DeviceIdentifier const& pci_device_identifier) const
{
    if (kernel_command_line().disable_usb())
        return EPERM;

    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::RaspberryPi
        || pci_device_identifier.hardware_id().device_id != PCI::DeviceID::RaspberryPiRP1)
        return ENOTSUP;

    return RP1::try_to_initialize_xhci_controllers(pci_device_identifier);
}

}
