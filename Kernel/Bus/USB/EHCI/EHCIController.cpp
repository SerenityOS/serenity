/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Bus/USB/EHCI/EHCIController.h>

namespace Kernel::USB::EHCI {

EHCIController::EHCIController(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<Memory::Region> register_region, VirtualAddress register_base_address)
    : PCI::Device(pci_device_identifier)
    , m_register_region(move(register_region))
{
    m_cap_regs = bit_cast<CapabilityRegisters const*>(register_base_address.get());
    m_op_regs = bit_cast<OperationalRegisters volatile*>(register_base_address.get() + m_cap_regs->capability_length);
}

ErrorOr<void> EHCIController::initialize()
{
    dmesgln_pci(*this, "Controller found {} @ {}", PCI::get_hardware_id(device_identifier()), device_identifier().address());
    dmesgln_pci(*this, "Version {}.{}", m_cap_regs->interface_version.major, m_cap_regs->interface_version.minor);
    u8 n_ports = m_cap_regs->structural_parameters.n_ports;
    dmesgln_pci(*this, "NPorts: {}", n_ports);
    u8 n_cc = m_cap_regs->structural_parameters.n_companion_controllers;
    u8 n_pcc = m_cap_regs->structural_parameters.n_ports_per_companion_controller;
    dmesgln_pci(*this, "Companion Controllers: {}", n_cc);
    dmesgln_pci(*this, "Ports per Companion Controllers: {}", n_pcc);

    if (n_ports > n_cc * n_pcc) {
        dmesgln_pci(*this, "Warning: Not all ports of the EHCI controller are addressable via companion controllers");
        dmesgln_pci(*this, "         Some USB 2.0 ports might not be functional");
    }

    u8 EECP = m_cap_regs->capability_parameters.ehci_extended_capabilities_pointer;
    if (EECP) {
        SpinlockLocker locker(device_identifier().operation_lock());
        auto legacy_support = bit_cast<LegacySupport>(PCI::read32_locked(device_identifier(), PCI::RegisterOffset { EECP }));
        if (legacy_support.HC_BIOS_owned_semaphore)
            dmesgln_pci(*this, "Warning: EHCI controller is BIOS owned");
    }

    // FIXME: Decide which Interrupts we want
    // FIXME: Detect and switch on 64 bit support
    // FIXME: Allocate and initialize Task Lists
    //        * Synchronous
    //        * Asynchronous
    //        * Leave space for for the actual list items
    //          and IO scratch space in case we cannot use the buffer from the request

    // FIXME: Initialize the controller and start it
    //       * Setup the root hub emulation
    //       * Enable Software routing (CF)
    //       * Maybe configure port power

    return {};
}

}
