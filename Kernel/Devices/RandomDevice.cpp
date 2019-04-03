#include "RandomDevice.h"
#include "Limits.h"
#include <AK/StdLibExtras.h>

RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

RandomDevice::~RandomDevice()
{
}

// Simple rand() and srand() borrowed from the POSIX standard:

static unsigned long next = 1;

#define MY_RAND_MAX 32767
int RandomDevice::random_value()
{
    next = next * 1103515245 + 12345;
    return((unsigned)(next/((MY_RAND_MAX + 1) * 2)) % (MY_RAND_MAX + 1));
}

float RandomDevice::random_percentage()
{
    return (float)random_value() / (float)MY_RAND_MAX;
}

#if 0
static void mysrand(unsigned seed)
{
    next = seed;
}
#endif

bool RandomDevice::can_read(Process&) const
{
    return true;
}

ssize_t RandomDevice::read(Process&, byte* buffer, ssize_t size)
{
    const int range = 'z' - 'a';
    ssize_t nread = min(size, GoodBufferSize);
    for (ssize_t i = 0; i < nread; ++i) {
        dword r = random_value() % range;
        buffer[i] = (byte)('a' + r);
    }
    return nread;
}

ssize_t RandomDevice::write(Process&, const byte*, ssize_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(GoodBufferSize, size);
}

