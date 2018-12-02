#include "RandomDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>

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
static int myrand()
{
    next = next * 1103515245 + 12345;
    return((unsigned)(next/((MY_RAND_MAX + 1) * 2)) % (MY_RAND_MAX + 1));
}

#if 0
static void mysrand(unsigned seed)
{
    next = seed;
}
#endif

bool RandomDevice::has_data_available_for_reading() const
{
    return true;
}

ssize_t RandomDevice::read(byte* buffer, size_t bufferSize)
{
    const int range = 'z' - 'a';
    ssize_t nread = min(bufferSize, GoodBufferSize);
    for (ssize_t i = 0; i < nread; ++i) {
        dword r = myrand() % range;
        buffer[i] = 'a' + r;
    }
    return nread;
}

ssize_t RandomDevice::write(const byte*, size_t bufferSize)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(GoodBufferSize, bufferSize);
}

