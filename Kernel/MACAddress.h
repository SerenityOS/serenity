#pragma once

#include <AK/Assertions.h>
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

private:
    byte m_data[6];
};

static_assert(sizeof(MACAddress) == 6);
