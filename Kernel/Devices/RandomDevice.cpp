#include "RandomDevice.h"
#include <AK/StdLibExtras.h>

RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

RandomDevice::~RandomDevice()
{
}

static dword next = 1;

#define MY_RAND_MAX 4294967295U
dword RandomDevice::random_value()
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

bool RandomDevice::can_read(FileDescriptor&) const
{
    return true;
}

ssize_t RandomDevice::read(FileDescriptor&, byte* buffer, ssize_t size)
{
    const int range = 'z' - 'a';
    ssize_t nread = min(size, PAGE_SIZE);
    for (ssize_t i = 0; i < nread; ++i) {
        dword r = random_value() % range;
        buffer[i] = (byte)('a' + r);
    }
    return nread;
}

ssize_t RandomDevice::write(FileDescriptor&, const byte*, ssize_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(PAGE_SIZE, size);
}

