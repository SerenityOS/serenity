#include "CharacterDevice.h"

CharacterDevice::~CharacterDevice()
{
}

OwnPtr<FileHandle> CharacterDevice::open(int options)
{
    //VirtualFileSystem::the().open()
}
