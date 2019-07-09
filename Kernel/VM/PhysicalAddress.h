#pragma once

#include <AK/Types.h>

class PhysicalAddress {
public:
    PhysicalAddress() {}
    explicit PhysicalAddress(u32 address)
        : m_address(address)
    {
    }

    PhysicalAddress offset(u32 o) const { return PhysicalAddress(m_address + o); }
    u32 get() const { return m_address; }
    void set(u32 address) { m_address = address; }
    void mask(u32 m) { m_address &= m; }

    bool is_null() const { return m_address == 0; }

    u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    u32 page_base() const { return m_address & 0xfffff000; }

    bool operator==(const PhysicalAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const PhysicalAddress& other) const { return m_address != other.m_address; }
    bool operator>(const PhysicalAddress& other) const { return m_address > other.m_address; }
    bool operator>=(const PhysicalAddress& other) const { return m_address >= other.m_address; }
    bool operator<(const PhysicalAddress& other) const { return m_address < other.m_address; }
    bool operator<=(const PhysicalAddress& other) const { return m_address <= other.m_address; }

private:
    u32 m_address { 0 };
};
