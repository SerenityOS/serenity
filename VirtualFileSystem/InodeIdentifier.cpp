#include "InodeIdentifier.h"
#include "FileSystem.h"

ByteBuffer InodeIdentifier::read_entire_file() const
{
    if (!fs())
        return { };
    return fs()->read_entire_inode(*this, nullptr);
}
