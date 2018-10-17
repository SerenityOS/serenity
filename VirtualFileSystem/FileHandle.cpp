#include "FileHandle.h"
#include "FileSystem.h"
#include "CharacterDevice.h"
#include "sys-errno.h"
#include "UnixTypes.h"

FileHandle::FileHandle(RetainPtr<VirtualFileSystem::Node>&& vnode)
    : m_vnode(move(vnode))
{
}

FileHandle::~FileHandle()
{
}

#ifndef SERENITY_KERNEL
bool additionWouldOverflow(Unix::off_t a, Unix::off_t b)
{
    ASSERT(a > 0);
    uint64_t ua = a;
    return (ua + b) > maxFileOffset;
}
#endif

int FileHandle::stat(Unix::stat* buffer)
{
    if (!m_vnode)
        return -EBADF;

    auto metadata = m_vnode->inode.metadata();
    if (!metadata.isValid())
        return -EIO;

    buffer->st_dev = 0; // FIXME
    buffer->st_ino = metadata.inode.index();
    buffer->st_mode = metadata.mode;
    buffer->st_nlink = metadata.linkCount;
    buffer->st_uid = metadata.uid;
    buffer->st_gid = metadata.gid;
    buffer->st_rdev = 0; // FIXME
    buffer->st_size = metadata.size;
    buffer->st_blksize = metadata.blockSize;
    buffer->st_blocks = metadata.blockCount;
    buffer->st_atime = metadata.atime;
    buffer->st_mtime = metadata.mtime;
    buffer->st_ctime = metadata.ctime;
    return 0;
}

Unix::off_t FileHandle::seek(Unix::off_t offset, int whence)
{
    if (!m_vnode)
        return -EBADF;

    // FIXME: The file type should be cached on the vnode.
    //        It's silly that we have to do a full metadata lookup here.
    auto metadata = m_vnode->inode.metadata();
    if (!metadata.isValid())
        return -EIO;

    if (metadata.isSocket() || metadata.isFIFO())
        return -ESPIPE;

    Unix::off_t newOffset;

    switch (whence) {
    case SEEK_SET:
        newOffset = offset;
        break;
    case SEEK_CUR:
        newOffset = m_currentOffset + offset;
#ifndef SERENITY_KERNEL
        if (additionWouldOverflow(m_currentOffset, offset))
            return -EOVERFLOW;
#endif
        if (newOffset < 0)
            return -EINVAL;
        break;
    case SEEK_END:
        // FIXME: Implement!
        notImplemented();
        newOffset = 0;
        break;
    default:
        return -EINVAL;
    }

    m_currentOffset = newOffset;
    return m_currentOffset;
}

Unix::ssize_t FileHandle::read(byte* buffer, Unix::size_t count)
{
    if (m_vnode->isCharacterDevice()) {
        // FIXME: What should happen to m_currentOffset?
        return m_vnode->characterDevice()->read(buffer, count);
    }
    Unix::ssize_t nread = m_vnode->fileSystem()->readInodeBytes(m_vnode->inode, m_currentOffset, count, buffer);
    m_currentOffset += nread;
    return nread;
}

ByteBuffer FileHandle::readEntireFile()
{
    if (m_vnode->isCharacterDevice()) {
        auto buffer = ByteBuffer::createUninitialized(1024);
        Unix::ssize_t nread = m_vnode->characterDevice()->read(buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    return m_vnode->fileSystem()->readEntireInode(m_vnode->inode);
}

