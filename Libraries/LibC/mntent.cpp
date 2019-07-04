#include <assert.h>
#include <mntent.h>

extern "C" {

struct mntent* getmntent(FILE*)
{
    ASSERT_NOT_REACHED();
    return nullptr;
}
}
