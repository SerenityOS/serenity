#include "CharacterDevice.h"

CharacterDevice::~CharacterDevice()
{
}

RetainPtr<FileHandle> CharacterDevice::open(int options)
{
    return VirtualFileSystem::the().open(*this, options);
}
