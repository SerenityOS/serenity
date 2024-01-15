/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Span.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::USB::xHCI {

ErrorOr<NonnullLockRefPtr<xHCIController>> xHCIController::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::enable_bus_mastering(pci_device_identifier);
    PCI::enable_memory_space(pci_device_identifier);
    PCI::enable_io_space(pci_device_identifier);

    auto pci_bar_address = TRY(PCI::get_bar_address(pci_device_identifier, BaseRegister));
    auto pci_bar_space_size = PCI::get_BAR_space_size(pci_device_identifier, BaseRegister);
    auto register_region = TRY(MM.allocate_kernel_region(pci_bar_address, pci_bar_space_size, {}, Memory::Region::Access::ReadWrite));

    auto controller = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) xHCIController(pci_device_identifier, move(register_region))));

    TRY(controller->initialize());

    return controller;
}

xHCIController::xHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<Memory::Region> register_region)
    : PCI::Device(pci_device_identifier)
    , m_register_region(move(register_region))
{
    // FIXME: Bounds checks
    m_cap_regs = bit_cast<CapabilityRegisters const volatile*>(m_register_region->vaddr().get());
    m_op_regs = bit_cast<OperationalRegisters volatile*>(m_register_region->vaddr().get() + m_cap_regs->caplength);
    m_doorbell_regs = bit_cast<DoorbellRegister volatile*>(m_register_region->vaddr().get() + m_cap_regs->doorbell_offset);
    m_runtime_regs = bit_cast<RuntimeRegisters volatile*>(m_register_region->vaddr().get() + m_cap_regs->runtime_register_space_offset);
}

ErrorOr<void> xHCIController::initialize()
{
    dmesgln_pci(*this, "Controller found {} @ {}", PCI::get_hardware_id(device_identifier()), device_identifier().address());
    dmesgln_pci(*this, "Version {}.{}", (u8)m_cap_regs->hci_version_major, (u8)m_cap_regs->hci_version_minor);
    dmesgln_pci(*this, "Ports: {}", (u8)m_cap_regs->structural_parameters1.number_of_ports);
    dmesgln_pci(*this, "Device Slots: {}", (u8)m_cap_regs->structural_parameters1.max_device_slots);
    dmesgln_pci(*this, "Interrupters: {}", (u8)m_cap_regs->structural_parameters1.max_interrupters);
    return {};
}

}
