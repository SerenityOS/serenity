#pragma once

#define PACKED __attribute__ ((packed))
#define NORETURN __attribute__ ((noreturn))
#define PURE __attribute__ ((pure))
#define PUBLIC
#define PRIVATE static

template <typename T>
T&& move(T& arg)
{
    return static_cast<T&&>(arg);
}

template<typename T>
T min(T a, T b)
{
    return (a < b) ? a : b;
}

template<typename T>
T max(T a, T b)
{
    return (a > b) ? a : b;
}

template<typename T, typename U>
void swap(T& a, U& b)
{
    U tmp = move((U&)a);
    a = (T&&)move(b);
    b = move(tmp);
}

template<typename T>
struct identity {
    typedef T type;
};
template<typename T>
T&& forward(typename identity<T>::type&& param)
{ return static_cast<typename identity<T>::type&&>(param); }

template<typename T, typename U>
T exchange(T& a, U&& b)
{
    T tmp = move(a);
    a = move(b);
    return tmp;
}

typedef unsigned char BYTE;
typedef unsigned short WORD;
typedef unsigned int DWORD;

typedef DWORD __u32;
typedef WORD __u16;
typedef BYTE __u8;
typedef int __s32;
typedef short __s16;

typedef DWORD uid_t;
typedef DWORD gid_t;
typedef int pid_t;
typedef DWORD time_t;
typedef DWORD size_t;

struct FarPtr {
    DWORD offset { 0 };
    WORD selector { 0 };
} PACKED;
