/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibCore/IODevice.h>

namespace Core {

class IODeviceStreamReader {
public:
    IODeviceStreamReader(IODevice& device)
        : m_device(device)
    {
    }

    bool handle_read_failure()
    {
        return exchange(m_had_failure, false);
    }

    template<typename T>
    IODeviceStreamReader& operator>>(T& value)
    {
        int nread = m_device.read((u8*)&value, sizeof(T));
        VERIFY(nread == sizeof(T));
        if (nread != sizeof(T))
            m_had_failure = true;
        return *this;
    }

private:
    IODevice& m_device;
    bool m_had_failure { false };
};

}
