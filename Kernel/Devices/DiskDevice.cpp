#include <Kernel/Devices/DiskDevice.h>

DiskDevice::DiskDevice(int major, int minor)
    : BlockDevice(major, minor)
{
}

DiskDevice::~DiskDevice()
{
}

bool DiskDevice::read(DiskOffset offset, unsigned length, u8* out) const
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    u32 first_block = offset / block_size();
    u32 end_block = (offset + length) / block_size();
    return const_cast<DiskDevice*>(this)->read_blocks(first_block, end_block - first_block, out);
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const u8* in)
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    u32 first_block = offset / block_size();
    u32 end_block = (offset + length) / block_size();
    ASSERT(first_block <= 0xffffffff);
    ASSERT(end_block <= 0xffffffff);
    return write_blocks(first_block, end_block - first_block, in);
}
