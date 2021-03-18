/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Atomic.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/SATADiskDevice.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

NonnullRefPtr<AHCIController> AHCIController::initialize(PCI::Address address)
{
    return adopt(*new AHCIController(address));
}

bool AHCIController::reset()
{
    hba().control_regs.ghc = 1;

    full_memory_barrier();
    size_t retry = 0;

    while (true) {
        if (retry > 1000)
            return false;
        if (!(hba().control_regs.ghc & 1))
            break;
        IO::delay(1000);
        retry++;
    }
    // The HBA is locked or hung if we waited more than 1 second!
    return true;
}

bool AHCIController::shutdown()
{
    TODO();
}

size_t AHCIController::devices_count() const
{
    size_t count = 0;
    for (auto& port_handler : m_handlers) {
        port_handler.enumerate_ports([&](const AHCIPort& port) {
            if (port.connected_device())
                count++;
        });
    }
    return count;
}

void AHCIController::start_request(const StorageDevice&, AsyncBlockDeviceRequest&)
{
    VERIFY_NOT_REACHED();
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
    return static_cast<volatile AHCI::HBA&>(*(volatile AHCI::HBA*)(m_hba_region->vaddr().as_ptr()));
}

AHCIController::AHCIController(PCI::Address address)
    : StorageController()
    , PCI::DeviceController(address)
    , m_hba_region(hba_region())
    , m_capabilities(capabilities())
{
    initialize();
}

AHCI::HBADefinedCapabilities AHCIController::capabilities() const
{
    u32 capabilities = hba().control_regs.cap;
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
        (capabilities & (u32)(AHCI::HBACapabilities::S64A)) != 0
    };
}

NonnullOwnPtr<Region> AHCIController::hba_region() const
{
    auto region = MM.allocate_kernel_region(PhysicalAddress(PCI::get_BAR5(pci_address())).page_base(), page_round_up(sizeof(AHCI::HBA)), "AHCI HBA", Region::Access::Read | Region::Access::Write);
    return region.release_nonnull();
}

AHCIController::~AHCIController()
{
}

void AHCIController::initialize()
{
    if (kernel_command_line().ahci_reset_mode() != AHCIResetMode::None) {
        if (!reset()) {
            dmesgln("{}: AHCI controller reset failed", pci_address());
            return;
        }
        dmesgln("{}: AHCI controller reset", pci_address());
    }
    dbgln("{}: AHCI command list entries count - {}", pci_address(), hba_capabilities().max_command_list_entries_count);
    hba().control_regs.ghc = 0x80000000; // Ensure that HBA knows we are AHCI aware.
    PCI::enable_interrupt_line(pci_address());
    PCI::enable_bus_mastering(pci_address());
    enable_global_interrupts();
    m_handlers.append(AHCIPortHandler::create(*this, PCI::get_interrupt_line(pci_address()),
        AHCI::MaskedBitField((volatile u32&)(hba().control_regs.pi))));
}

void AHCIController::disable_global_interrupts() const
{
    hba().control_regs.ghc = hba().control_regs.ghc & 0xfffffffd;
}
void AHCIController::enable_global_interrupts() const
{
    hba().control_regs.ghc = hba().control_regs.ghc | (1 << 1);
}

RefPtr<StorageDevice> AHCIController::device_by_port(u32 port_index) const
{
    for (auto& port_handler : m_handlers) {
        if (!port_handler.is_responsible_for_port_index(port_index))
            continue;

        auto port = port_handler.port_at_index(port_index);
        if (!port)
            return nullptr;
        return port->connected_device();
    }
    return nullptr;
}

RefPtr<StorageDevice> AHCIController::device(u32 index) const
{
    NonnullRefPtrVector<StorageDevice> connected_devices;
    for (size_t index = 0; index < (size_t)(hba().control_regs.cap & 0x1F); index++) {
        auto checked_device = device_by_port(index);
        if (checked_device.is_null())
            continue;
        connected_devices.append(checked_device.release_nonnull());
    }
    if (index >= connected_devices.size())
        return nullptr;
    return connected_devices[index];
}

}
