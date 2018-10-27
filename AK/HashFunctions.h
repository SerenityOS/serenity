#pragma once

#include "Types.h"

inline unsigned intHash(dword key)
{
    key += ~(key << 15);
    key ^= (key >> 10);
    key += (key << 3);
    key ^= (key >> 6);
    key += ~(key << 11);
    key ^= (key >> 16);
    return key;
}

inline unsigned pairIntHash(dword key1, dword key2)
{
    return intHash((intHash(key1) * 209) ^ (intHash(key2 * 413)));
}

