#include <strings.h>
#include <assert.h>

extern "C" {

int strcasecmp(const char*, const char*)
{
    assert(false);
}

int strncasecmp(const char*, const char*, size_t)
{
    assert(false);
}

}
