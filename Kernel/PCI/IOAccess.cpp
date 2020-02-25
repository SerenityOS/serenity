/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/PCI/IOAccess.h>
#include <LibBareMetal/IO.h>

namespace Kernel {

void PCI::IOAccess::initialize()
{
    if (!PCI::Access::is_initialized())
        new PCI::IOAccess();
}

PCI::IOAccess::IOAccess()
{
    kprintf("PCI: Using IO Mechanism for PCI Configuartion Space Access\n");
}

u8 PCI::IOAccess::read8_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}

u16 PCI::IOAccess::read16_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}

u32 PCI::IOAccess::read32_field(Address address, u32 field)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}

void PCI::IOAccess::write8_field(Address address, u32 field, u8 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out8(PCI_VALUE_PORT + (field & 3), value);
}
void PCI::IOAccess::write16_field(Address address, u32 field, u16 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out16(PCI_VALUE_PORT + (field & 2), value);
}
void PCI::IOAccess::write32_field(Address address, u32 field, u32 value)
{
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out32(PCI_VALUE_PORT, value);
}

void PCI::IOAccess::enumerate_all(Function<void(Address, ID)>& callback)
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
