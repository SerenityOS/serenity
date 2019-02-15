#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>

Device::~Device()
{
}

RetainPtr<FileDescriptor> Device::open(int& error, int options)
{
    return VFS::the().open(*this, error, options);
}

void Device::close()
{
}

int Device::ioctl(Process&, unsigned, unsigned)
{
    return -ENOTTY;
}
