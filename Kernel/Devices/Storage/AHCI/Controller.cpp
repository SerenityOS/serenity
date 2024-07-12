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
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/BarMapping.h>
#include <Kernel/Devices/Storage/AHCI/Controller.h>
#include <Kernel/Devices/Storage/AHCI/InterruptHandler.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AHCIController>> AHCIController::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto controller = adopt_ref_if_nonnull(new (nothrow) AHCIController(pci_device_identifier)).release_nonnull();
    TRY(controller->initialize_hba(pci_device_identifier));
    return controller;
}

ErrorOr<void> AHCIController::reset()
{
    dmesgln_pci(*this, "{}: AHCI controller reset", device_identifier().address());
    {
        SpinlockLocker locker(m_hba_control_lock);
        hba().control_regs.ghc = 1;

        dbgln_if(AHCI_DEBUG, "{}: AHCI Controller reset", device_identifier().address());

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

size_t AHCIController::devices_count() const
{
    SpinlockLocker locker(m_hba_control_lock);
    size_t count = 0;
    for (auto port : m_ports) {
        if (port && port->connected_device())
            count++;
    }
    return count;
}

void AHCIController::start_request(ATA::Address address, AsyncBlockDeviceRequest& request)
{
    auto port = m_ports[address.port];
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

UNMAP_AFTER_INIT AHCIController::AHCIController(PCI::DeviceIdentifier const& pci_device_identifier)
    : StorageController(StorageManagement::generate_relative_ahci_controller_id({}))
    , PCI::Device(const_cast<PCI::DeviceIdentifier&>(pci_device_identifier))
{
}

UNMAP_AFTER_INIT AHCI::HBADefinedCapabilities AHCIController::capabilities() const
{
    u32 capabilities = hba().control_regs.cap;
    u32 extended_capabilities = hba().control_regs.cap2;

    dbgln_if(AHCI_DEBUG, "{}: AHCI Controller Capabilities = {:#08x}, Extended Capabilities = {:#08x}", device_identifier().address(), capabilities, extended_capabilities);

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

UNMAP_AFTER_INIT ErrorOr<Memory::TypedMapping<AHCI::HBA volatile>> AHCIController::map_default_hba_region(PCI::DeviceIdentifier const& pci_device_identifier)
{
    return PCI::map_bar<AHCI::HBA volatile>(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR5);
}

AHCIController::~AHCIController() = default;

UNMAP_AFTER_INIT ErrorOr<void> AHCIController::initialize_hba(PCI::DeviceIdentifier const& pci_device_identifier)
{
    m_hba_mapping = TRY(map_default_hba_region(pci_device_identifier));
    m_hba_capabilities = capabilities();

    u32 version = hba().control_regs.version;

    hba().control_regs.ghc = 0x80000000; // Ensure that HBA knows we are AHCI aware.
    PCI::enable_bus_mastering(device_identifier());
    TRY(reserve_irqs(1, true));
    auto irq = MUST(allocate_irq(0));
    enable_global_interrupts();

    auto implemented_ports = AHCI::MaskedBitField((u32 volatile&)(hba().control_regs.pi));
    m_irq_handler = TRY(AHCIInterruptHandler::create(*this, irq, implemented_ports));
    TRY(reset());

    dbgln_if(AHCI_DEBUG, "{}: AHCI Controller Version = {:#08x}", device_identifier().address(), version);
    dbgln("{}: AHCI command list entries count - {}", device_identifier().address(), m_hba_capabilities.max_command_list_entries_count);

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

LockRefPtr<StorageDevice> AHCIController::device_by_port(u32 port_index) const
{
    SpinlockLocker locker(m_hba_control_lock);
    auto port = m_ports[port_index];
    if (!port)
        return {};
    SpinlockLocker port_hard_locker(port->m_hard_lock);

    // FIXME: Remove this once we get rid of this hacky method in the future.
    auto device = port->connected_device();
    if (!device)
        return nullptr;
    return *device;
}

LockRefPtr<StorageDevice> AHCIController::device(u32 index) const
{
    Vector<NonnullLockRefPtr<StorageDevice>> connected_devices;
    u32 pi = hba().control_regs.pi;
    u32 bit = bit_scan_forward(pi);
    while (bit) {
        dbgln_if(AHCI_DEBUG, "Checking implemented port {}, pi {:b}", bit - 1, pi);
        pi &= ~(1u << (bit - 1));
        auto checked_device = device_by_port(bit - 1);
        bit = bit_scan_forward(pi);
        if (checked_device.is_null())
            continue;
        connected_devices.append(checked_device.release_nonnull());
    }
    dbgln_if(AHCI_DEBUG, "Connected device count: {}, Index: {}", connected_devices.size(), index);
    if (index >= connected_devices.size())
        return nullptr;
    return connected_devices[index];
}

}
