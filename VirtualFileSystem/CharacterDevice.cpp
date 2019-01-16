#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>

CharacterDevice::~CharacterDevice()
{
    ASSERT_NOT_REACHED();
}

RetainPtr<FileDescriptor> CharacterDevice::open(int options)
{
    return VFS::the().open(*this, options);
}

int CharacterDevice::ioctl(Process&, unsigned, unsigned)
{
    return -ENOTTY;
}
