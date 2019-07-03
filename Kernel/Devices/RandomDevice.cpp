#include "RandomDevice.h"
#include <AK/StdLibExtras.h>

RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

RandomDevice::~RandomDevice()
{
}

static u32 next = 1;

#define MY_RAND_MAX 4294967295U
u32 RandomDevice::random_value()
{
    next = next * 1103515245 + 12345;
    return next;
}

#if 0
static void mysrand(unsigned seed)
{
    next = seed;
}
#endif

bool RandomDevice::can_read(FileDescription&) const
{
    return true;
}

ssize_t RandomDevice::read(FileDescription&, u8* buffer, ssize_t size)
{
    const int range = 'z' - 'a';
    ssize_t nread = min(size, PAGE_SIZE);
    for (ssize_t i = 0; i < nread; ++i) {
        u32 r = random_value() % range;
        buffer[i] = (u8)('a' + r);
    }
    return nread;
}

ssize_t RandomDevice::write(FileDescription&, const u8*, ssize_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(PAGE_SIZE, size);
}
