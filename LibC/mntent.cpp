#include <mntent.h>
#include <assert.h>

extern "C" {

struct mntent* getmntent(FILE*)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}

}

