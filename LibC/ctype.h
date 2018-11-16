#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

ALWAYS_INLINE int isascii(int ch)
{
    return (ch & ~0x7f) == 0;
}

ALWAYS_INLINE int isspace(int ch)
{
    return ch == ' ' || ch == '\f' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\v';
}

ALWAYS_INLINE int islower(int c)
{
    return c >= 'a' && c <= 'z';
}

ALWAYS_INLINE int isupper(int c)
{
    return c >= 'A' && c <= 'Z';
}

ALWAYS_INLINE int tolower(int c)
{
    if (isupper(c))
        return c | 0x20;
    return c;
}

ALWAYS_INLINE int toupper(int c)
{
    if (islower(c))
        return c & ~0x20;
    return c;
}

ALWAYS_INLINE int isdigit(int c)
{
    return c >= '0' && c <= '9';
}

int isalpha(int c);
int isalnum(int c);
int ispunct(int c);
int isprint(int c);
int iscntrl(int c);

__END_DECLS
