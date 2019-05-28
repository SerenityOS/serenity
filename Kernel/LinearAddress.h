#pragma once

#include <AK/Types.h>

class LinearAddress {
public:
    LinearAddress() {}
    explicit LinearAddress(dword address)
        : m_address(address)
    {
    }

    bool is_null() const { return m_address == 0; }

    LinearAddress offset(dword o) const { return LinearAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool operator<=(const LinearAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const LinearAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const LinearAddress& other) const { return m_address > other.m_address; }
    bool operator<(const LinearAddress& other) const { return m_address < other.m_address; }
    bool operator==(const LinearAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const LinearAddress& other) const { return m_address != other.m_address; }

    byte* as_ptr() { return reinterpret_cast<byte*>(m_address); }
    const byte* as_ptr() const { return reinterpret_cast<const byte*>(m_address); }

    dword page_base() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};

inline LinearAddress operator-(const LinearAddress& a, const LinearAddress& b)
{
    return LinearAddress(a.get() - b.get());
}
