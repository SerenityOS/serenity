#pragma once

#include <AK/Assertions.h>
#include <AK/AKString.h>
#include <AK/Types.h>
#include <Kernel/StdLib.h>

class [[gnu::packed]] MACAddress {
public:
    MACAddress() { }
    MACAddress(const byte data[6])
    {
        memcpy(m_data, data, 6);
    }
    ~MACAddress() { }

    byte operator[](int i) const
    {
        ASSERT(i >= 0 && i < 6);
        return m_data[i];
    }

    String to_string() const
    {
        return String::format("%b:%b:%b:%b:%b:%b", m_data[0], m_data[1], m_data[2], m_data[3], m_data[4], m_data[5]);
    }

private:
    byte m_data[6];
};

static_assert(sizeof(MACAddress) == 6);
