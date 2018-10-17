#include "DiskDevice.h"

DiskDevice::DiskDevice()
{
}

DiskDevice::~DiskDevice()
{
}

bool DiskDevice::read(DiskOffset offset, unsigned length, byte* out) const
{
    ASSERT((offset % blockSize()) == 0);
    ASSERT((length % blockSize()) == 0);
    dword firstBlock = offset / blockSize();
    dword endBlock = (offset + length) / blockSize();
    byte* outptr = out;
    unsigned remainingCount = length;
    for (unsigned bi = firstBlock; bi < endBlock; ++bi) {
        if (!readBlock(bi, outptr))
            return false;
        outptr += blockSize();
    }
    return true;
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const byte* in)
{
    ASSERT((offset % blockSize()) == 0);
    ASSERT((length % blockSize()) == 0);
    dword firstBlock = offset / blockSize();
    dword endBlock = (offset + length) / blockSize();
    ASSERT(firstBlock <= 0xffffffff);
    ASSERT(endBlock <= 0xffffffff);
    const byte* inptr = in;
    for (unsigned bi = firstBlock; bi < endBlock; ++bi) {
        if (!writeBlock(bi, inptr))
            return false;
        inptr += blockSize();
    }
    return true;
}

