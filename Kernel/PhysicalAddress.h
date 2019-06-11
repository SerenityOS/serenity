#pragma once

class PhysicalAddress {
public:
    PhysicalAddress() {}
    explicit PhysicalAddress(dword address)
        : m_address(address)
    {
    }

    PhysicalAddress offset(dword o) const { return PhysicalAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool is_null() const { return m_address == 0; }

    byte* as_ptr() { return reinterpret_cast<byte*>(m_address); }
    const byte* as_ptr() const { return reinterpret_cast<const byte*>(m_address); }

    dword page_base() const { return m_address & 0xfffff000; }

    bool operator==(const PhysicalAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const PhysicalAddress& other) const { return m_address != other.m_address; }
    bool operator>(const PhysicalAddress& other) const { return m_address > other.m_address; }
    bool operator>=(const PhysicalAddress& other) const { return m_address >= other.m_address; }
    bool operator<(const PhysicalAddress& other) const { return m_address < other.m_address; }
    bool operator<=(const PhysicalAddress& other) const { return m_address <= other.m_address; }

private:
    dword m_address { 0 };
};
