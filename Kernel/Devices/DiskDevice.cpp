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
    return const_cast<DiskDevice*>(this)->read_blocks(first_block, end_block - first_block, out);
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const byte* in)
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    dword first_block = offset / block_size();
    dword end_block = (offset + length) / block_size();
    ASSERT(first_block <= 0xffffffff);
    ASSERT(end_block <= 0xffffffff);
    return write_blocks(first_block, end_block - first_block, in);
}

