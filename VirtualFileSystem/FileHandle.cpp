#include "FileHandle.h"
#include "FileSystem.h"
#include "CharacterDevice.h"
#include "sys-errno.h"

FileHandle::FileHandle(RetainPtr<VirtualFileSystem::Node>&& vnode)
    : m_vnode(std::move(vnode))
{
}

FileHandle::~FileHandle()
{
}

bool additionWouldOverflow(FileOffset a, FileOffset b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > maxFileOffset;
}

FileOffset FileHandle::lseek(FileOffset offset, SeekType seekType)
{
    if (!m_vnode)
        return -EBADF;

    // FIXME: The file type should be cached on the vnode.
    //        It's silly that we have to do a full metadata lookup here.
    auto metadata = m_vnode->inode.metadata();
    if (metadata.isSocket() || metadata.isFIFO())
        return -ESPIPE;

    FileOffset newOffset;

    switch (seekType) {
    case SeekType::Absolute:
        newOffset = offset;
        break;
    case SeekType::RelativeToCurrent:
        newOffset = m_currentOffset + offset;
        if (additionWouldOverflow(m_currentOffset, offset))
            return -EOVERFLOW;
        if (newOffset < 0)
            return -EINVAL;
        break;
    case SeekType::RelativeToEnd:
        // FIXME: Implement!
        notImplemented();
        break;
    default:
        return -EINVAL;
    }

    m_currentOffset = newOffset;
    return m_currentOffset;
}

ssize_t FileHandle::read(byte* buffer, size_t count)
{
    if (m_vnode->isCharacterDevice()) {
        // FIXME: What should happen to m_currentOffset?
        return m_vnode->characterDevice()->read(buffer, count);
    }
    ssize_t nread = m_vnode->fileSystem()->readInodeBytes(m_vnode->inode, m_currentOffset, count, buffer);
    m_currentOffset += nread;
    return nread;
}

ByteBuffer FileHandle::readEntireFile()
{
    if (m_vnode->isCharacterDevice()) {
        auto buffer = ByteBuffer::createUninitialized(1024);
        ssize_t nread = m_vnode->characterDevice()->read(buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    return m_vnode->fileSystem()->readInode(m_vnode->inode);
}

