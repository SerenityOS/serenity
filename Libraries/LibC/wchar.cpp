#include <wchar.h>

extern "C" {

size_t wcslen(const wchar_t* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

wchar_t* wcscpy(wchar_t* dest, const wchar_t* src)
{
    wchar_t* originalDest = dest;
    while ((*dest++ = *src++) != '\0')
        ;
    return originalDest;
}

int wcscmp(const wchar_t* s1, const wchar_t* s2)
{
    while (*s1 == *s2++)
        if (*s1++ == 0)
            return 0;
    return *(const wchar_t*)s1 - *(const wchar_t*)--s2;
}

wchar_t* wcschr(const wchar_t* str, int c)
{
    wchar_t ch = c;
    for (;; ++str) {
        if (*str == ch)
            return const_cast<wchar_t*>(str);
        if (!*str)
            return nullptr;
    }
}

}
