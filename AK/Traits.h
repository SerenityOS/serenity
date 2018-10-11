#pragma once

#include <cstdio>

namespace AK {

template<typename T>
struct Traits 
{
};

template<>
struct Traits<int> {
    static unsigned hash(int i) { return i; }
    static void dump(int i) { printf("%d", i); }
};

template<>
struct Traits<unsigned> {
    static unsigned hash(unsigned u) { return u; }
    static void dump(unsigned u) { printf("%u", u); }
};

template<typename T>
struct Traits<T*> {
    static unsigned hash(const T* p) { return (unsigned)p; }
    static void dump(const T* p) { printf("%p", p); }
};

}

