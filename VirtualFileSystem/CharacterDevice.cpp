#include "CharacterDevice.h"

CharacterDevice::~CharacterDevice()
{
}

OwnPtr<FileHandle> CharacterDevice::open(int options)
{
    return VirtualFileSystem::the().open(*this, options);
}
