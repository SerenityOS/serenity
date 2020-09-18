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

#include <Kernel/IO.h>
#include <Kernel/PCI/IOAccess.h>

namespace Kernel {
namespace PCI {

void IOAccess::initialize()
{
    if (!Access::is_initialized())
        new IOAccess();
}

IOAccess::IOAccess()
{
    klog() << "PCI: Using I/O instructions for PCI configuration space access";
    enumerate_hardware([&](const Address& address, ID id) {
        m_physical_ids.append({ address, id });
    });
}

u8 IOAccess::read8_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}

u16 IOAccess::read16_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}

u32 IOAccess::read32_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}

void IOAccess::write8_field(Address address, u32 field, u8 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out8(PCI_VALUE_PORT + (field & 3), value);
}
void IOAccess::write16_field(Address address, u32 field, u16 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out16(PCI_VALUE_PORT + (field & 2), value);
}
void IOAccess::write32_field(Address address, u32 field, u32 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out32(PCI_VALUE_PORT, value);
}

void IOAccess::enumerate_hardware(Function<void(Address, ID)> callback)
{
    // Single PCI host controller.
    if ((read8_field(Address(), PCI_HEADER_TYPE) & 0x80) == 0) {
        enumerate_bus(-1, 0, callback);
        return;
    }

    // Multiple PCI host controllers.
    for (u8 function = 0; function < 8; ++function) {
        if (read16_field(Address(0, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
            break;
        enumerate_bus(-1, function, callback);
    }
}

}
}
