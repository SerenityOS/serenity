/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <ctype.h>
#include <string.h>
#include <strings.h>

extern "C" {

void bzero(void* dest, size_t n)
{
    memset(dest, 0, n);
}

void bcopy(void const* src, void* dest, size_t n)
{
    memmove(dest, src, n);
}

static char foldcase(char ch)
{
    if (isalpha(ch))
        return tolower(ch);
    return ch;
}

int strcasecmp(char const* s1, char const* s2)
{
    for (; foldcase(*s1) == foldcase(*s2); ++s1, ++s2) {
        if (*s1 == 0)
            return 0;
    }
    return foldcase(*(const unsigned char*)s1) < foldcase(*(const unsigned char*)s2) ? -1 : 1;
}

int strncasecmp(char const* s1, char const* s2, size_t n)
{
    if (!n)
        return 0;
    do {
        if (foldcase(*s1) != foldcase(*s2++))
            return foldcase(*(const unsigned char*)s1) - foldcase(*(const unsigned char*)--s2);
        if (*s1++ == 0)
            break;
    } while (--n);
    return 0;
}
}
