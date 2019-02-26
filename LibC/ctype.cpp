#include <ctype.h>
#include <string.h>

int __tolower(int c)
{
    if (c >= 'A' && c <= 'Z')
        return c | 0x20;
    return c;
}

int __toupper(int c)
{
    if (c >= 'a' && c <= 'z')
        return c & ~0x20;
    return c;
}
