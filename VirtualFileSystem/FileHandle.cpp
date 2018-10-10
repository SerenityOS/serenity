#include "FileHandle.h"
#include "FileSystem.h"

FileHandle::FileHandle(RetainPtr<VirtualFileSystem::Node>&& vnode)
    : m_vnode(std::move(vnode))
{
}

FileHandle::~FileHandle()
{
}

ByteBuffer FileHandle::read() const
{
    return m_vnode->fileSystem()->readInode(m_vnode->inode);
}

