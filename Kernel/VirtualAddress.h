#pragma once

#include <AK/Types.h>

class VirtualAddress {
public:
    VirtualAddress() {}
    explicit VirtualAddress(dword address)
        : m_address(address)
    {
    }

    bool is_null() const { return m_address == 0; }

    VirtualAddress offset(dword o) const { return VirtualAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool operator<=(const VirtualAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const VirtualAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const VirtualAddress& other) const { return m_address > other.m_address; }
    bool operator<(const VirtualAddress& other) const { return m_address < other.m_address; }
    bool operator==(const VirtualAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const VirtualAddress& other) const { return m_address != other.m_address; }

    byte* as_ptr() { return reinterpret_cast<byte*>(m_address); }
    const byte* as_ptr() const { return reinterpret_cast<const byte*>(m_address); }

    dword page_base() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};

inline VirtualAddress operator-(const VirtualAddress& a, const VirtualAddress& b)
{
    return VirtualAddress(a.get() - b.get());
}
