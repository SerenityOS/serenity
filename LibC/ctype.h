#pragma once

#include <sys/cdefs.h>
#include <string.h>

__BEGIN_DECLS

ALWAYS_INLINE int __isascii(int ch)
{
    return (ch & ~0x7f) == 0;
}

ALWAYS_INLINE int __isspace(int ch)
{
    return ch == ' ' || ch == '\f' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\v';
}

ALWAYS_INLINE int __islower(int c)
{
    return c >= 'a' && c <= 'z';
}

ALWAYS_INLINE int __isupper(int c)
{
    return c >= 'A' && c <= 'Z';
}

ALWAYS_INLINE int __tolower(int c)
{
    if (__isupper(c))
        return c | 0x20;
    return c;
}

ALWAYS_INLINE int __toupper(int c)
{
    if (__islower(c))
        return c & ~0x20;
    return c;
}

ALWAYS_INLINE int __isdigit(int c)
{
    return c >= '0' && c <= '9';
}

ALWAYS_INLINE int __ispunct(int c)
{
    const char* punctuation_characters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    return !!strchr(punctuation_characters, c);
}

ALWAYS_INLINE int __isprint(int c)
{
    return c >= 0x20 && c != 0x7f;
}

ALWAYS_INLINE int __isalpha(int c)
{
    return __isupper(c) || __islower(c);
}

ALWAYS_INLINE int __isalnum(int c)
{
    return __isalpha(c) || __isdigit(c);
}

ALWAYS_INLINE int __iscntrl(int c)
{
    return (c >= 0 && c <= 0x1f) || c == 0x7f;
}

#define isascii(c) __isascii(c)
#define isspace(c) __isspace(c)
#define islower(c) __islower(c)
#define isupper(c) __isupper(c)
#define tolower(c) __tolower(c)
#define toupper(c) __toupper(c)
#define isdigit(c) __isdigit(c)
#define ispunct(c) __ispunct(c)
#define isprint(c) __isprint(c)
#define isalpha(c) __isalpha(c)
#define isalnum(c) __isalnum(c)
#define iscntrl(c) __iscntrl(c)

__END_DECLS
