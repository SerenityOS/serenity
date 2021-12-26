#pragma once

#include <AK/Types.h>

template<typename T>
T convert_between_host_and_network(T host_value)
{
    if constexpr (sizeof(T) == 4) {
        auto* s = (byte*)&host_value;
        return (dword)(s[0] << 24 | s[1] << 16 | s[2] << 8 | s[3]);
    }
    if constexpr (sizeof(T) == 2) {
        auto* s = (byte*)&host_value;
        return (word)(s[0] << 8 | s[1]);
    }
    if constexpr (sizeof(T) == 1)
        return host_value;
}

template<typename T>
class [[gnu::packed]] NetworkOrdered {
public:
    NetworkOrdered()
        : m_network_value(0)
    {
    }

    NetworkOrdered(const T& host_value)
        : m_network_value(convert_between_host_and_network(host_value))
    {
    }

    NetworkOrdered(const NetworkOrdered& other)
        : m_network_value(other.m_network_value)
    {
    }

    NetworkOrdered& operator=(const NetworkOrdered& other)
    {
        m_network_value = other.m_network_value;
        return *this;
    }

    operator T() const { return convert_between_host_and_network(m_network_value); }

private:
    T m_network_value { 0 };
};
