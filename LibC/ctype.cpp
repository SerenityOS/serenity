#include <ctype.h>
#include <string.h>

int ispunct(int c)
{
    const char* punctuation_characters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    return !!strchr(punctuation_characters, c);
}

int isprint(int c)
{
    return isdigit(c) || isupper(c) || islower(c) || ispunct(c) || isspace(c);
}

int isalnum(int c)
{
    return isalpha(c) || isdigit(c);
}

int isalpha(int c)
{
    return isupper(c) || islower(c);
}

int iscntrl(int c)
{
    return (c >= 0 && c <= 0x1f) || c == 0x7f;
}
