#include <mntent.h>
#include <assert.h>

extern "C" {

struct mntent* getmntent(FILE* stream)
{
    assert(false);
    return nullptr;
}

}

