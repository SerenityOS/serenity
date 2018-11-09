#include <mntent.h>
#include <assert.h>

extern "C" {

struct mntent* getmntent(FILE*)
{
    assert(false);
    return nullptr;
}

}

