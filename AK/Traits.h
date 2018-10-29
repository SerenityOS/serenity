#pragma once

#include "kstdio.h"
#include "HashFunctions.h"

namespace AK {

template<typename T>
struct Traits 
{
};

template<>
struct Traits<int> {
    static unsigned hash(int i) { return intHash(i); }
    static void dump(int i) { kprintf("%d", i); }
};

template<>
struct Traits<unsigned> {
    static unsigned hash(unsigned u) { return intHash(u); }
    static void dump(unsigned u) { kprintf("%u", u); }
};

template<typename T>
struct Traits<T*> {
    static unsigned hash(const T* p)
    {
#ifdef SERENITY
        return intHash((dword)p);
#else
        return intHash((unsigned long long)p & 0xffffffff);
#endif
    }
    static void dump(const T* p) { kprintf("%p", p); }
};

}

