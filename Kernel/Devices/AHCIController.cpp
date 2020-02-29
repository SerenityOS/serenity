/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include <Kernel/Devices/AHCIController.h>
#include <Kernel/Devices/AHCIDiskDevice.h>
#include <Kernel/Devices/AHCIPort.h>
#include <Kernel/Devices/NullDevice.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>

namespace Kernel {

#define AHCI_PRIMARY_IRQ 16

#define AHCI_GHC 0x04
#define AHCI_PI 0x0C
#define AHCI_VS 0x10

#define PCI_Mass_Storage_Class 0x1
#define PCI_ATA_Controller_Subclass 0x6

#define READ32_HBA(base, member) *(u32*)(base.get() + member)

void AHCIController::create()
{
    PCI::Address pci_address;

    PCI::enumerate_all([&pci_address](const PCI::Address& address, PCI::ID) {
        int class_id = PCI::get_class(address);
        int subclass_id = PCI::get_subclass(address);

        if (class_id == PCI_Mass_Storage_Class
            && subclass_id == PCI_ATA_Controller_Subclass) {
            klog() << "AHCIController: Controller found!";
            pci_address = address;
        }
    });

    new AHCIController(pci_address);
}

static AHCIController* s_the;

AHCIController::AHCIController(PCI::Address address)
    : PCI::Device(address, PCI::get_interrupt_line(address))
    , m_abar_region(MM.allocate_kernel_region(
          PhysicalAddress(PCI::get_BAR5(address)),
          PAGE_ROUND_UP(PCI::get_BAR_Space_Size(address, 5)), "AHCI ABAR",
          Region::Access::Read | Region::Access::Write))
    , m_has_fatal_error(false)
{
    s_the = this;

    initialize();
    probe_ports();
}

inline RefPtr<AHCIDiskDevice> AHCIController::first_device()
{
    for (u32 i = 0; i < m_port_count; i++) {
        auto& port = m_ports[i];
        if (port != nullptr)
            return port->disk_device();
    }

    return nullptr;
}

AHCIController& AHCIController::the()
{
    ASSERT(s_the);
    return *s_the;
}

void AHCIController::handle_irq(const RegisterState&)
{
#ifdef DEBUG_AHCI
    klog() << "AHCI: Interrupt " << m_reg->is;
#endif

    u32 is = m_reg->is;
    for (u32 i = 0; i < m_port_count; i++) {
        // If bit is set, port has an interrupt pending
        if (is & 0x1 && m_ports[i] != nullptr) {
            auto& port = m_ports[i];
            bool has_fatal = port->handle_irq();

            if (has_fatal) {
                fatal_error(i);
                return;
            }
        }

        is >>= 1;
    }
}

void AHCIController::fatal_error(u32 port_index)
{
    klog() << "AHCIController: Fatal error "
           << "detected in port "
           << port_index;

    m_has_fatal_error = true;
}

void AHCIController::initialize()
{
    // Setup data pointers
    m_abar = m_abar_region->vaddr();
    m_reg = (ABARReg*)m_abar.get();

    m_reg->ghc = 1;
    klog() << "AHCIController: Version "
           << ((m_reg->vs >> 16) & 0xFFFF) << "."
           << ((m_reg->vs >> 8) & 0xFF) << (m_reg->vs & 0xFF);
}

void AHCIController::probe_ports()
{
    // Make a copy of PI
    u32 pi = m_reg->pi;

    for (u32 i = 0; i < m_port_count; i++) {
        if (pi & 1) {
            m_ports[i] = make<AHCIPort>(
                m_abar.offset(0x100 + i * 0x80), i);

#ifdef DEBUG_AHCI
            if (port->type() == AHCIPort::Type::SATA) {
                klog() << "AHCIChannel: Found SATA device on port " << i;
            }
#endif
        }

        pi >>= 1;
    }
}

}
