#pragma once

#include "HashFunctions.h"
#include "kstdio.h"

namespace AK {

template<typename T>
struct GenericTraits {
    using PeekType = T;
    static constexpr bool is_trivial() { return false; }
    static bool equals(const T& a, const T& b) { return a == b; }
};

template<typename T>
struct Traits : public GenericTraits<T> {
};

template<>
struct Traits<int> : public GenericTraits<int> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(int i) { return int_hash(i); }
    static void dump(int i) { kprintf("%d", i); }
};

template<>
struct Traits<unsigned> : public GenericTraits<unsigned> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(unsigned u) { return int_hash(u); }
    static void dump(unsigned u) { kprintf("%u", u); }
};

template<>
struct Traits<u16> : public GenericTraits<u16> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(u16 u) { return int_hash(u); }
    static void dump(u16 u) { kprintf("%u", u); }
};

template<>
struct Traits<u64> : public GenericTraits<u64> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(u64 u) { return u64_hash(u); }
    static void dump(u64 u) { kprintf("%u", (unsigned)u); } // FIXME: Implement unsigned long long printf specifier, instead of this sadness :(
};

template<>
struct Traits<char> : public GenericTraits<char> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(char c) { return int_hash(c); }
    static void dump(char c) { kprintf("%c", c); }
};

template<typename T>
struct Traits<T*> : public GenericTraits<T*> {
    static unsigned hash(const T* p)
    {
        return int_hash((unsigned)(__PTRDIFF_TYPE__)p);
    }
    static constexpr bool is_trivial() { return true; }
    static void dump(const T* p) { kprintf("%p", p); }
    static bool equals(const T* a, const T* b) { return a == b; }
};

}
