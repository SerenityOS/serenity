#include <assert.h>
#include <dlfcn.h>

extern "C" {

int dlclose(void*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

char* dlerror()
{
    ASSERT_NOT_REACHED();
    return nullptr;
}

void* dlopen(const char*, int)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}

void* dlsym(void*, const char*)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}
}
