/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/USB/xHCI/PCIxHCIController.h>
#include <Kernel/Bus/USB/xHCI/xHCIInterrupter.h>

namespace Kernel::USB {

ErrorOr<NonnullLockRefPtr<PCIxHCIController>> PCIxHCIController::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::enable_bus_mastering(pci_device_identifier);
    PCI::enable_memory_space(pci_device_identifier);

    auto registers_mapping = TRY(PCI::map_bar<u8>(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));
    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PCIxHCIController(pci_device_identifier, move(registers_mapping))));

    auto interrupt_type = TRY(controller->reserve_irqs(1, true)); // TODO: Support more than one interrupter using MSI/MSI-X
    controller->m_using_message_signalled_interrupts = interrupt_type != PCI::InterruptType::PIN;

    TRY(controller->initialize());
    return controller;
}

UNMAP_AFTER_INIT PCIxHCIController::PCIxHCIController(PCI::DeviceIdentifier const& pci_device_identifier, Memory::TypedMapping<u8> registers_mapping)
    : xHCIController(move(registers_mapping))
    , PCI::Device(pci_device_identifier)
{
    if (device_identifier().hardware_id().vendor_id == PCI::VendorID::Intel)
        intel_quirk_enable_xhci_ports();
}

void PCIxHCIController::intel_quirk_enable_xhci_ports()
{
    // Intel chipsets that include both xHCI and EHCI USB controllers default to configuring their USB ports to be attached to the EHCI controller,
    // Attach them to the xHCI controller instead.
    bool ehci_controller_found = false;
    MUST(PCI::enumerate([&ehci_controller_found](PCI::DeviceIdentifier const& device_identifier) {
        if (device_identifier.hardware_id().vendor_id != PCI::VendorID::Intel
            || device_identifier.class_code() != PCI::ClassID::SerialBus
            || device_identifier.subclass_code() != PCI::SerialBus::SubclassID::USB
            || device_identifier.prog_if() != PCI::SerialBus::USBProgIf::EHCI)
            return;
        ehci_controller_found = true;
    }));
    if (!ehci_controller_found)
        return;
    dmesgln_xhci("Switching Intel chipset USB ports to xHCI instead of EHCI");
    SpinlockLocker const locker(device_identifier().operation_lock());
    // Enable USB3 Ports
    PCI::write32_locked(device_identifier(), intel_xhci_usb3_port_super_speed_enable_offset, PCI::read32_locked(device_identifier(), intel_xhci_usb3_port_routing_mask_offset));
    // Enable USB2 Ports
    PCI::write32_locked(device_identifier(), intel_xhci_usb2_port_routing_offset, PCI::read32_locked(device_identifier(), intel_xhci_usb2_port_routing_mask_offset));
}

ErrorOr<NonnullOwnPtr<GenericInterruptHandler>> PCIxHCIController::create_interrupter(u16 interrupter_id)
{
    return TRY(xHCIPCIInterrupter::create(*this, interrupter_id));
}

}
