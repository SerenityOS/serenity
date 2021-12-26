#pragma once

#include <AK/Assertions.h>
#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/StdLib.h>

class [[gnu::packed]] IPv4Address {
public:
    IPv4Address() { }
    IPv4Address(const byte data[4])
    {
        memcpy(m_data, data, 4);
    }
    IPv4Address(byte a, byte b, byte c, byte d)
    {
        m_data[0] = a;
        m_data[1] = b;
        m_data[2] = c;
        m_data[3] = d;
    }
    ~IPv4Address() { }

    byte operator[](int i) const
    {
        ASSERT(i >= 0 && i < 4);
        return m_data[i];
    }

    String to_string() const
    {
        return String::format("%u.%u.%u.%u", m_data[0], m_data[1], m_data[2], m_data[3]);
    }

    bool operator==(const IPv4Address& other) const { return m_data_as_dword == other.m_data_as_dword; }
    bool operator!=(const IPv4Address& other) const { return m_data_as_dword != other.m_data_as_dword; }

private:
    union {
        byte m_data[4];
        dword m_data_as_dword;
    };
};

static_assert(sizeof(IPv4Address) == 4);

namespace AK {

template<>
struct Traits<IPv4Address> {
    static unsigned hash(const IPv4Address& address) { return string_hash((const char*)&address, sizeof(address)); }
    static void dump(const IPv4Address& address) { kprintf("%s", address.to_string().characters()); }
};

}
