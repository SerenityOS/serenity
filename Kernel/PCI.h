#pragma once

#include <AK/Function.h>
#include <AK/Types.h>

namespace PCI {

struct ID {
    word vendor_id { 0 };
    word device_id { 0 };

    bool is_null() const { return !vendor_id && !device_id; }

    bool operator==(const ID& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
};

struct Address {
    Address() {}
    Address(byte bus, byte slot, byte function)
        : m_bus(bus)
        , m_slot(slot)
        , m_function(function)
    {
    }

    bool is_null() const { return !m_bus && !m_slot && !m_function; }
    operator bool() const { return !is_null(); }

    byte bus() const { return m_bus; }
    byte slot() const { return m_slot; }
    byte function() const { return m_function; }

    dword io_address_for_field(byte field) const
    {
        return 0x80000000u | (m_bus << 16u) | (m_slot << 11u) | (m_function << 8u) | (field & 0xfc);
    }

private:
    byte m_bus { 0 };
    byte m_slot { 0 };
    byte m_function { 0 };
};

void enumerate_all(Function<void(Address, ID)>);
byte get_interrupt_line(Address);
dword get_BAR0(Address);
dword get_BAR1(Address);
dword get_BAR2(Address);
dword get_BAR3(Address);
dword get_BAR4(Address);
dword get_BAR5(Address);
void enable_bus_mastering(Address);

}
