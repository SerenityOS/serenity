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
