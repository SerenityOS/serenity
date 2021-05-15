/*
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Storage/IntelAHCIController.h>
#include <Kernel/Storage/IntelAHCIPort.h>

namespace Kernel {

NonnullRefPtr<AHCIController> IntelAHCIController::initialize(PCI::Address address)
{
    return adopt_ref(*new IntelAHCIController(address));
}

IntelAHCIController::IntelAHCIController(PCI::Address address)
    : AHCIController(address, false)
{
    initialize();
}

void IntelAHCIController::initialize()
{
    if (kernel_command_line().ahci_reset_mode() != AHCIResetMode::None) {
        if (!AHCIController::reset()) {
            dmesgln("{}: AHCI controller reset failed", pci_address());
            return;
        }
        dmesgln("{}: AHCI controller reset", pci_address());
    }
    dbgln("{}: AHCI command list entries count - {}", pci_address(), AHCIController::hba_capabilities().max_command_list_entries_count);

    u32 version = AHCIController::hba().control_regs.version;
    dbgln_if(AHCI_DEBUG, "{}: AHCI Controller Version = 0x{:08x}", pci_address(), version);

    AHCIController::hba().control_regs.ghc = 0x80000000; // Ensure that HBA knows we are AHCI aware.
    PCI::enable_interrupt_line(pci_address());
    PCI::enable_bus_mastering(pci_address());
    AHCIController::enable_global_interrupts();
    AK::Vector<NonnullRefPtr<AHCIPort>> ports;

    m_handlers.append(AHCIPortHandler::create(*this, PCI::get_interrupt_line(pci_address()),
        AHCI::MaskedBitField((volatile u32&)(hba().control_regs.pi)), IntelAHCIPort::create));
}

}
