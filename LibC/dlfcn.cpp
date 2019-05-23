#include "dlfcn.h"
#include <assert.h>

int dlclose(void*)
{
    ASSERT_NOT_REACHED();
}

char *dlerror()
{
    ASSERT_NOT_REACHED();
}

void *dlopen(const char*, int)
{
    ASSERT_NOT_REACHED();
}

void *dlsym(void*, const char*)
{
    ASSERT_NOT_REACHED();
}
