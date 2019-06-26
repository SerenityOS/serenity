#pragma once

#include <AK/Types.h>

template<typename T>
[[gnu::always_inline]] inline T convert_between_host_and_network(T value)
{
    if constexpr (sizeof(T) == 8)
        return __builtin_bswap64(value);
    if constexpr (sizeof(T) == 4)
        return __builtin_bswap32(value);
    if constexpr (sizeof(T) == 2)
        return __builtin_bswap16(value);
    if constexpr (sizeof(T) == 1)
        return value;
}

template<typename T>
class [[gnu::packed]] NetworkOrdered
{
public:
    NetworkOrdered() {}
    NetworkOrdered(const T& host_value)
        : m_network_value(convert_between_host_and_network(host_value))
    {
    }

    operator T() const { return convert_between_host_and_network(m_network_value); }

private:
    T m_network_value { 0 };
};
