#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

[[gnu::always_inline]] inline int isascii(int ch)
{
    return (ch & ~0x7f) == 0;
}

[[gnu::always_inline]] inline int isspace(int ch)
{
    return ch == ' ' || ch == '\f' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\v';
}

[[gnu::always_inline]] inline int islower(int c)
{
    return c >= 'a' && c <= 'z';
}

[[gnu::always_inline]] inline int isupper(int c)
{
    return c >= 'A' && c <= 'Z';
}

[[gnu::always_inline]] inline int tolower(int c)
{
    if (isupper(c))
        return c | 0x20;
    return c;
}

[[gnu::always_inline]] inline int toupper(int c)
{
    if (islower(c))
        return c & ~0x20;
    return c;
}

[[gnu::always_inline]] inline int isdigit(int c)
{
    return c >= '0' && c <= '9';
}

int isalpha(int c);
int isalnum(int c);
int ispunct(int c);
int isprint(int c);
int iscntrl(int c);

__END_DECLS
