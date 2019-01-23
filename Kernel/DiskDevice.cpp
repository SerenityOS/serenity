#include "DiskDevice.h"

DiskDevice::DiskDevice()
{
}

DiskDevice::~DiskDevice()
{
}

bool DiskDevice::read(DiskOffset offset, unsigned length, byte* out) const
{
    //kprintf("DD::read %u x%u\n", offset, length);
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    dword firstBlock = offset / block_size();
    dword endBlock = (offset + length) / block_size();
    byte* outptr = out;
    for (unsigned bi = firstBlock; bi < endBlock; ++bi) {
        if (!read_block(bi, outptr))
            return false;
        outptr += block_size();
    }
    return true;
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const byte* in)
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    dword firstBlock = offset / block_size();
    dword endBlock = (offset + length) / block_size();
    ASSERT(firstBlock <= 0xffffffff);
    ASSERT(endBlock <= 0xffffffff);
    const byte* inptr = in;
    for (unsigned bi = firstBlock; bi < endBlock; ++bi) {
        if (!write_block(bi, inptr))
            return false;
        inptr += block_size();
    }
    return true;
}

