#pragma once

#include <AK/StdLibExtras.h>
#include <LibCore/CIODevice.h>

class CIODeviceStreamReader {
public:
    CIODeviceStreamReader(CIODevice& device)
        : m_device(device)
    {
    }

    bool handle_read_failure()
    {
        return exchange(m_had_failure, false);
    }

    template<typename T>
    CIODeviceStreamReader& operator>>(T& value)
    {
        int nread = m_device.read((u8*)&value, sizeof(T));
        ASSERT(nread == sizeof(T));
        if (nread != sizeof(T))
            m_had_failure = true;
        return *this;
    }

private:
    CIODevice& m_device;
    bool m_had_failure { false };
};
