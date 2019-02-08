#include <locale.h>
#include <assert.h>
#include <stdio.h>

extern "C" {

char* setlocale(int category, const char* locale)
{
    dbgprintf("FIXME(LibC): setlocale(%d, %s)\n", category, locale);
    return nullptr;
}

}
