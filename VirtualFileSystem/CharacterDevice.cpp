#include "CharacterDevice.h"

CharacterDevice::~CharacterDevice()
{
}

RetainPtr<FileDescriptor> CharacterDevice::open(int options)
{
    return VFS::the().open(*this, options);
}
