#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/StdLib.h>

class MACAddress {
public:
    MACAddress() { }
    MACAddress(const byte data[6])
        : m_valid(true)
    {
        memcpy(m_data, data, 6);
    }
    ~MACAddress() { }

    bool is_valid() const { return m_valid; }
    byte operator[](int i) const
    {
        ASSERT(i >= 0 && i < 6);
        return m_data[i];
    }

private:
    byte m_data[6];
    bool m_valid { false };
};
