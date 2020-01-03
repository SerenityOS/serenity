#pragma once

#include <AK/Types.h>

// NOTE: These API's are primarily about expressing intent/needs in the calling code.
//       We don't make any guarantees about actual fastness or goodness yet.

void get_fast_random_bytes(u8*, size_t);
void get_good_random_bytes(u8*, size_t);

template<typename T>
inline T get_fast_random()
{
    T value;
    get_fast_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

template<typename T>
inline T get_good_random()
{
    T value;
    get_good_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

