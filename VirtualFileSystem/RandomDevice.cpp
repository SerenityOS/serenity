#include "RandomDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>
#include <cstring>
#include <cstdio>

RandomDevice::RandomDevice()
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

static void mysrand(unsigned seed)
{
    next = seed;
}

Unix::ssize_t RandomDevice::read(byte* buffer, Unix::size_t bufferSize)
{
    const int range = 'z' - 'a';
    Unix::ssize_t nread = min(bufferSize, GoodBufferSize);
    for (Unix::ssize_t i = 0; i < nread; ++i) {
        double r = ((double)myrand() / (double)MY_RAND_MAX) * (double)range;
        buffer[i] = 'a' + r;
    }
    return nread;
}

Unix::ssize_t RandomDevice::write(const byte*, Unix::size_t bufferSize)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(GoodBufferSize, bufferSize);
}

