/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/PCI/DeviceController.h>

namespace Kernel {
namespace PCI {

DeviceController::DeviceController(Address address)
    : m_pci_address(address)
{
}

bool DeviceController::is_msi_capable() const
{
    for (auto capability : PCI::get_physical_id(pci_address()).capabilities()) {
        if (capability.id() == PCI_CAPABILITY_MSI)
            return true;
    }
    return false;
}
bool DeviceController::is_msix_capable() const
{
    for (auto capability : PCI::get_physical_id(pci_address()).capabilities()) {
        if (capability.id() == PCI_CAPABILITY_MSIX)
            return true;
    }
    return false;
}

void DeviceController::enable_pin_based_interrupts() const
{
    PCI::enable_interrupt_line(pci_address());
}
void DeviceController::disable_pin_based_interrupts() const
{
    PCI::disable_interrupt_line(pci_address());
}

void DeviceController::enable_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::disable_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::enable_extended_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::disable_extended_message_signalled_interrupts()
{
    TODO();
}

}
}
