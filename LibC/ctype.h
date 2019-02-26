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

int __tolower(int);
int __toupper(int);

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

ALWAYS_INLINE int __isxdigit(int c)
{
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
}

ALWAYS_INLINE int __isgraph(int c)
{
    return __isalnum(c) || __ispunct(c);
}

#ifdef __cplusplus
#define __CTYPE_FUNC(name) static inline int name(int c) { return __ ## name(c); }

__CTYPE_FUNC(isascii)
__CTYPE_FUNC(isspace)
__CTYPE_FUNC(islower)
__CTYPE_FUNC(isupper)
__CTYPE_FUNC(tolower)
__CTYPE_FUNC(toupper)
__CTYPE_FUNC(isdigit)
__CTYPE_FUNC(ispunct)
__CTYPE_FUNC(isprint)
__CTYPE_FUNC(isalpha)
__CTYPE_FUNC(isalnum)
__CTYPE_FUNC(iscntrl)
__CTYPE_FUNC(isxdigit)
__CTYPE_FUNC(isgraph)
#else
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
#define isxdigit(c) __isxdigit(c)
#define isgraph(c) __isgraph(c)
#endif

__END_DECLS
