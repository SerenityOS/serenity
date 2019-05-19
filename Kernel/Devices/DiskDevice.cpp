#include <Kernel/Devices/DiskDevice.h>

DiskDevice::DiskDevice()
{
}

DiskDevice::~DiskDevice()
{
}

bool DiskDevice::read(DiskOffset offset, unsigned length, byte* out) const
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    dword first_block = offset / block_size();
    dword end_block = (offset + length) / block_size();
    byte* outptr = out;

    return const_cast<DiskDevice*>(this)->read_blocks(first_block, end_block - first_block, outptr);
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const byte* in)
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    dword first_block = offset / block_size();
    dword end_block = (offset + length) / block_size();
    ASSERT(first_block <= 0xffffffff);
    ASSERT(end_block <= 0xffffffff);
    const byte* inptr = in;
    for (unsigned bi = first_block; bi < end_block; ++bi) {
        if (!write_block(bi, inptr))
            return false;
        inptr += block_size();
    }
    return true;
}

