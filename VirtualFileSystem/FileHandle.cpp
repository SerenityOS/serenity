#include "FileHandle.h"
#include "FileSystem.h"
#include "CharacterDevice.h"

FileHandle::FileHandle(RetainPtr<VirtualFileSystem::Node>&& vnode)
    : m_vnode(std::move(vnode))
{
}

FileHandle::~FileHandle()
{
}

ByteBuffer FileHandle::read()
{
    if (m_vnode->isCharacterDevice()) {
        auto buffer = ByteBuffer::createUninitialized(1024);
        ssize_t nread = m_vnode->characterDevice()->read(buffer.pointer(), buffer.size());
        buffer.trim(nread);
        return buffer;
    }

    return m_vnode->fileSystem()->readInode(m_vnode->inode);
}

