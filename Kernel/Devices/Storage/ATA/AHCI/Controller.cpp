/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/BuiltinWrappers.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Devices/Storage/ATA/AHCI/Controller.h>
#include <Kernel/Devices/Storage/ATA/AHCI/InterruptHandler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<AHCIController>> AHCIController::initialize(PCI::Device& pci_device)
{
    auto controller = adopt_ref_if_nonnull(new (nothrow) AHCIController(pci_device)).release_nonnull();
    TRY(controller->initialize_hba());
    return controller;
}

ErrorOr<void> AHCIController::reset()
{
    dmesgln_pci(*m_pci_device, "{}: AHCI controller reset", m_pci_device->device_id().address());
    {
        SpinlockLocker locker(m_hba_control_lock);
        hba().control_regs.ghc = 1;

        dbgln_if(AHCI_DEBUG, "{}: AHCI Controller reset", m_pci_device->device_id().address());

        full_memory_barrier();
        size_t retry = 0;

        // Note: The HBA is locked or hung if we waited more than 1 second!
        while (true) {
            if (retry > 1000)
                return Error::from_errno(ETIMEDOUT);
            if (!(hba().control_regs.ghc & 1))
                break;
            microseconds_delay(1000);
            retry++;
        }
        // Note: Turn on AHCI HBA and Global HBA Interrupts.
        full_memory_barrier();
        hba().control_regs.ghc = (1 << 31) | (1 << 1);
        full_memory_barrier();
    }

    // Note: According to the AHCI spec the PI register indicates which ports are exposed by the HBA.
    // It is loaded by the BIOS. It indicates which ports that the HBA supports are available for software to use.
    // For example, on an HBA that supports 6 ports as indicated in CAP.NP, only ports 1 and 3 could be available,
    // with ports 0, 2, 4, and 5 being unavailable.
    // Which means that even without clearing the AHCI ports array, we are never able to encounter
    // a case that we would have stale left-over ports in there. We still clear the array
    // for the sake of clarity and completeness, as it doesn't harm anything anyway.
    m_ports.fill({});

    auto implemented_ports = AHCI::MaskedBitField((u32 volatile&)(hba().control_regs.pi));
    for (auto index : implemented_ports.to_vector()) {
        auto port = TRY(AHCIPort::create(*this, m_hba_capabilities, static_cast<volatile AHCI::PortRegisters&>(hba().port_regs[index]), index));
        m_ports[index] = port;
        port->reset();
    }
    return {};
}

void AHCIController::start_request(ATADevice const& device, AsyncBlockDeviceRequest& request)
{
    auto port = m_ports[device.ata_address().port];
    VERIFY(port);
    port->start_request(request);
}

void AHCIController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

volatile AHCI::PortRegisters& AHCIController::port(size_t port_number) const
{
    VERIFY(port_number < (size_t)AHCI::Limits::MaxPorts);
    return static_cast<volatile AHCI::PortRegisters&>(hba().port_regs[port_number]);
}

volatile AHCI::HBA& AHCIController::hba() const
{
    return const_cast<AHCI::HBA&>(*m_hba_mapping);
}

AHCIController::AHCIController(PCI::Device& device)
    : ATAController()
    , m_pci_device(device)
{
}

AHCI::HBADefinedCapabilities AHCIController::capabilities() const
{
    u32 capabilities = hba().control_regs.cap;
    u32 extended_capabilities = hba().control_regs.cap2;

    dbgln_if(AHCI_DEBUG, "{}: AHCI Controller Capabilities = {:#08x}, Extended Capabilities = {:#08x}", m_pci_device->device_id().address(), capabilities, extended_capabilities);

    return (AHCI::HBADefinedCapabilities) {
        (capabilities & 0b11111) + 1,
        ((capabilities >> 8) & 0b11111) + 1,
        (u8)((capabilities >> 20) & 0b1111),
        (capabilities & (u32)(AHCI::HBACapabilities::SXS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::EMS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::CCCS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::PSC)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SSC)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::PMD)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::FBSS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SPM)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SAM)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SCLO)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SAL)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SALP)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SSS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SMPS)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SSNTF)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::SNCQ)) != 0,
        (capabilities & (u32)(AHCI::HBACapabilities::S64A)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::BOH)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::NVMP)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::APST)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::SDS)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::SADM)) != 0,
        (extended_capabilities & (u32)(AHCI::HBACapabilitiesExtended::DESO)) != 0
    };
}

ErrorOr<Memory::TypedMapping<AHCI::HBA volatile>> AHCIController::map_default_hba_region()
{
    return Memory::map_typed_writable<AHCI::HBA volatile>(PhysicalAddress(m_pci_device->resources()[5].physical_memory_address()));
}

AHCIController::~AHCIController() = default;

ErrorOr<void> AHCIController::initialize_hba()
{
    m_hba_mapping = TRY(map_default_hba_region());
    m_hba_capabilities = capabilities();

    u32 version = hba().control_regs.version;

    hba().control_regs.ghc = 0x80000000; // Ensure that HBA knows we are AHCI aware.
    m_pci_device->enable_bus_mastering();
    TRY(m_pci_device->reserve_irqs(1, true));
    auto irq = MUST(m_pci_device->allocate_irq(0));
    enable_global_interrupts();

    auto implemented_ports = AHCI::MaskedBitField((u32 volatile&)(hba().control_regs.pi));
    m_irq_handler = TRY(AHCIInterruptHandler::create(*this, *m_pci_device, irq, implemented_ports));
    TRY(reset());

    dbgln_if(AHCI_DEBUG, "{}: AHCI Controller Version = {:#08x}", m_pci_device->device_id().address(), version);
    dbgln("{}: AHCI command list entries count - {}", m_pci_device->device_id().address(), m_hba_capabilities.max_command_list_entries_count);

    return {};
}

void AHCIController::handle_interrupt_for_port(Badge<AHCIInterruptHandler>, u32 port_index) const
{
    auto port = m_ports[port_index];
    VERIFY(port);
    port->handle_interrupt();
}

void AHCIController::disable_global_interrupts() const
{
    hba().control_regs.ghc = hba().control_regs.ghc & 0xfffffffd;
}
void AHCIController::enable_global_interrupts() const
{
    hba().control_regs.ghc = hba().control_regs.ghc | (1 << 1);
}

}
