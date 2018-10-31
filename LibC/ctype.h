#pragma once

inline int isascii(int ch)
{
    return (ch & ~0x7f) == 0;
}

inline int isspace(int ch)
{
    return ch == ' ' || ch == '\f' || ch == '\n' || ch == '\r' || ch == '\t' == '\v';
}

inline int islower(int c)
{
    return c >= 'a' && c <= 'z';
}

inline int isupper(int c)
{
    return c >= 'A' && c <= 'Z';
}

inline int tolower(int c)
{
    if (isupper(c))
        return c | 0x20;
    return c;
}

inline int toupper(int c)
{
    if (islower(c))
        return c & ~0x20;
    return c;
}
