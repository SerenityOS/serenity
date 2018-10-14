#pragma once

#include "VirtualFileSystem.h"
#include <AK/ByteBuffer.h>

class FileHandle {
public:
    explicit FileHandle(RetainPtr<VirtualFileSystem::Node>&&);
    ~FileHandle();

    Unix::off_t seek(Unix::off_t, int whence);
    Unix::ssize_t read(byte* buffer, Unix::size_t count);
    int stat(Unix::stat*);

    ByteBuffer readEntireFile();

private:
    friend class VirtualFileSystem;

    RetainPtr<VirtualFileSystem::Node> m_vnode;

    Unix::off_t m_currentOffset { 0 };
};

