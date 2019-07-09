#pragma once

#include <AK/Types.h>

class VirtualAddress {
public:
    VirtualAddress() {}
    explicit VirtualAddress(u32 address)
        : m_address(address)
    {
    }

    bool is_null() const { return m_address == 0; }
    bool is_page_aligned() const { return (m_address & 0xfff) == 0; }

    VirtualAddress offset(u32 o) const { return VirtualAddress(m_address + o); }
    u32 get() const { return m_address; }
    void set(u32 address) { m_address = address; }
    void mask(u32 m) { m_address &= m; }

    bool operator<=(const VirtualAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const VirtualAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const VirtualAddress& other) const { return m_address > other.m_address; }
    bool operator<(const VirtualAddress& other) const { return m_address < other.m_address; }
    bool operator==(const VirtualAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const VirtualAddress& other) const { return m_address != other.m_address; }

    u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    u32 page_base() const { return m_address & 0xfffff000; }

private:
    u32 m_address { 0 };
};

inline VirtualAddress operator-(const VirtualAddress& a, const VirtualAddress& b)
{
    return VirtualAddress(a.get() - b.get());
}
