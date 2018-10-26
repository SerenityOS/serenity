#include "InodeIdentifier.h"
#include "FileSystem.h"

ByteBuffer InodeIdentifier::readEntireFile() const
{
    if (!fileSystem())
        return { };
    return fileSystem()->readEntireInode(*this, nullptr);
}
