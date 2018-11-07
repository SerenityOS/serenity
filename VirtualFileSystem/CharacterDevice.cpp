#include "CharacterDevice.h"

CharacterDevice::~CharacterDevice()
{
}

RetainPtr<FileDescriptor> CharacterDevice::open(int options)
{
    return VirtualFileSystem::the().open(*this, options);
}
