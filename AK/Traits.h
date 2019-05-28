#pragma once

#include "HashFunctions.h"
#include "kstdio.h"

namespace AK {

template<typename T>
struct Traits {
};

template<>
struct Traits<int> {
    static unsigned hash(int i) { return int_hash(i); }
    static void dump(int i) { kprintf("%d", i); }
};

template<>
struct Traits<unsigned> {
    static unsigned hash(unsigned u) { return int_hash(u); }
    static void dump(unsigned u) { kprintf("%u", u); }
};

template<>
struct Traits<word> {
    static unsigned hash(unsigned u) { return int_hash(u); }
    static void dump(unsigned u) { kprintf("%u", u); }
};

template<typename T>
struct Traits<T*> {
    static unsigned hash(const T* p)
    {
        return int_hash((dword)p);
    }
    static void dump(const T* p) { kprintf("%p", p); }
};

}
