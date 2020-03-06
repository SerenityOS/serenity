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

#pragma once

#include <AK/String.h>
#include <Kernel/PCI/Definitions.h>

namespace Kernel {

class PCI::Access {
public:
    virtual void enumerate_all(Function<void(Address, ID)>&) = 0;
    virtual u8 get_interrupt_line(Address address) { return read8_field(address, PCI_INTERRUPT_LINE); }
    virtual u32 get_BAR0(Address address) { return read32_field(address, PCI_BAR0); }
    virtual u32 get_BAR1(Address address) { return read32_field(address, PCI_BAR1); }
    virtual u32 get_BAR2(Address address) { return read32_field(address, PCI_BAR2); }
    virtual u32 get_BAR3(Address address) { return read32_field(address, PCI_BAR3); }
    virtual u32 get_BAR4(Address address) { return read32_field(address, PCI_BAR4); }
    virtual u32 get_BAR5(Address address) { return read32_field(address, PCI_BAR5); }

    virtual size_t get_BAR_Space_Size(Address address, u8 bar_number)
    {
        // See PCI Spec 2.3, Page 222
        ASSERT(bar_number < 6);
        u8 field = (PCI_BAR0 + (bar_number << 2));
        u32 bar_reserved = read32_field(address, field);
        write32_field(address, field, 0xFFFFFFFF);
        u32 space_size = read32_field(address, field);
        write32_field(address, field, bar_reserved);
        space_size &= 0xfffffff0;
        space_size = (~space_size) + 1;
        return space_size;
    }
    virtual ID get_id(Address address) final;
    virtual void enable_interrupt_line(Address address) final
    {
        write16_field(address, PCI_COMMAND, read16_field(address, PCI_COMMAND) & ~(1 << 10));
    }
    virtual void disable_interrupt_line(Address address) final
    {
        write16_field(address, PCI_COMMAND, read16_field(address, PCI_COMMAND) | 1 << 10);
    }
    virtual u8 get_revision_id(Address address) { return read8_field(address, PCI_REVISION_ID); }
    virtual u8 get_subclass(Address address) { return read8_field(address, PCI_SUBCLASS); }
    virtual u8 get_class(Address address) { return read8_field(address, PCI_CLASS); }
    virtual u16 get_subsystem_id(Address address) { return read16_field(address, PCI_SUBSYSTEM_ID); }
    virtual u16 get_subsystem_vendor_id(Address address) { return read16_field(address, PCI_SUBSYSTEM_VENDOR_ID); }
    virtual u16 read_type(Address address) { return (read8_field(address, PCI_CLASS) << 8u) | read8_field(address, PCI_SUBCLASS); }

    virtual void enable_bus_mastering(Address) final;
    virtual void disable_bus_mastering(Address) final;

    virtual void enumerate_bus(int type, u8 bus, Function<void(Address, ID)>&) final;
    virtual void enumerate_functions(int type, u8 bus, u8 slot, u8 function, Function<void(Address, ID)>& callback) final;
    virtual void enumerate_slot(int type, u8 bus, u8 slot, Function<void(Address, ID)>& callback) final;

    static Access& the();
    static bool is_initialized();
    virtual uint32_t get_segments_count() = 0;
    virtual uint8_t get_segment_start_bus(u32 segment) = 0;
    virtual uint8_t get_segment_end_bus(u32 segment) = 0;
    virtual String get_access_type() = 0;

protected:
    Access();

    virtual u8 read8_field(Address address, u32 field) = 0;
    virtual u16 read16_field(Address address, u32 field) = 0;
    virtual u32 read32_field(Address address, u32 field) = 0;

    virtual void write8_field(Address address, u32 field, u8 value) = 0;
    virtual void write16_field(Address address, u32 field, u16 value) = 0;
    virtual void write32_field(Address address, u32 field, u32 value) = 0;
};

}
