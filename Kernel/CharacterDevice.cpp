#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>

CharacterDevice::~CharacterDevice()
{
}

RetainPtr<FileDescriptor> CharacterDevice::open(int& error, int options)
{
    return VFS::the().open(*this, error, options);
}

void CharacterDevice::close()
{
}

int CharacterDevice::ioctl(Process&, unsigned, unsigned)
{
    return -ENOTTY;
}
