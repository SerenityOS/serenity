#pragma once

#include "HashFunctions.h"
#include "kstdio.h"

namespace AK {

template<typename T>
struct GenericTraits {
    static bool equals(const T& a, const T& b) { return a == b; }
};

template<typename T>
struct Traits : public GenericTraits<T> {
};

template<>
struct Traits<int> : public GenericTraits<int> {
    static unsigned hash(int i) { return int_hash(i); }
    static void dump(int i) { kprintf("%d", i); }
};

template<>
struct Traits<unsigned> : public GenericTraits<unsigned> {
    static unsigned hash(unsigned u) { return int_hash(u); }
    static void dump(unsigned u) { kprintf("%u", u); }
};

template<>
struct Traits<u16> : public GenericTraits<u16> {
    static unsigned hash(u16 u) { return int_hash(u); }
    static void dump(u16 u) { kprintf("%u", u); }
};

template<typename T>
struct Traits<T*> {
    static unsigned hash(const T* p)
    {
        return int_hash((unsigned)(__PTRDIFF_TYPE__)p);
    }
    static void dump(const T* p) { kprintf("%p", p); }
    static bool equals(const T* a, const T* b) { return a == b; }
};

}
