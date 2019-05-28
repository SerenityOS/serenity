#pragma once

#include "Types.h"

inline unsigned int_hash(dword key)
{
    key += ~(key << 15);
    key ^= (key >> 10);
    key += (key << 3);
    key ^= (key >> 6);
    key += ~(key << 11);
    key ^= (key >> 16);
    return key;
}

inline unsigned pair_int_hash(dword key1, dword key2)
{
    return int_hash((int_hash(key1) * 209) ^ (int_hash(key2 * 413)));
}
