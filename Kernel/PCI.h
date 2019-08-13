#pragma once

#include <AK/Function.h>
#include <AK/Types.h>

namespace PCI {

struct ID {
    u16 vendor_id { 0 };
    u16 device_id { 0 };

    bool is_null() const { return !vendor_id && !device_id; }

    bool operator==(const ID& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
};

struct Address {
    Address() {}
    Address(u8 bus, u8 slot, u8 function)
        : m_bus(bus)
        , m_slot(slot)
        , m_function(function)
    {
    }

    bool is_null() const { return !m_bus && !m_slot && !m_function; }
    operator bool() const { return !is_null(); }

    u8 bus() const { return m_bus; }
    u8 slot() const { return m_slot; }
    u8 function() const { return m_function; }

    u32 io_address_for_field(u8 field) const
    {
        return 0x80000000u | (m_bus << 16u) | (m_slot << 11u) | (m_function << 8u) | (field & 0xfc);
    }

private:
    u8 m_bus { 0 };
    u8 m_slot { 0 };
    u8 m_function { 0 };
};

void enumerate_all(Function<void(Address, ID)>);
u8 get_interrupt_line(Address);
u32 get_BAR0(Address);
u32 get_BAR1(Address);
u32 get_BAR2(Address);
u32 get_BAR3(Address);
u32 get_BAR4(Address);
u32 get_BAR5(Address);
u8 get_revision_id(Address);
u8 get_subclass(Address);
u8 get_class(Address);
u16 get_subsystem_id(Address);
u16 get_subsystem_vendor_id(Address);
void enable_bus_mastering(Address);

}
