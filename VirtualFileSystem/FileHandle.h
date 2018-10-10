#pragma once

#include "VirtualFileSystem.h"
#include <AK/ByteBuffer.h>

class FileHandle {
public:
    explicit FileHandle(RetainPtr<VirtualFileSystem::Node>&&);
    ~FileHandle();

    ByteBuffer read() const;

private:
    friend class VirtualFileSystem;

    RetainPtr<VirtualFileSystem::Node> m_vnode;
};

