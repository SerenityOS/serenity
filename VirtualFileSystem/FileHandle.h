#pragma once

#include "VirtualFileSystem.h"
#include <AK/ByteBuffer.h>

enum class SeekType {
    Absolute,           // SEEK_SET
    RelativeToCurrent,  // SEEK_CUR
    RelativeToEnd,      // SEEK_END
};

class FileHandle {
public:
    explicit FileHandle(RetainPtr<VirtualFileSystem::Node>&&);
    ~FileHandle();

    FileOffset lseek(FileOffset, SeekType);
    ssize_t read(byte* buffer, size_t count);

    ByteBuffer readEntireFile();

private:
    friend class VirtualFileSystem;

    RetainPtr<VirtualFileSystem::Node> m_vnode;

    FileOffset m_currentOffset { 0 };
};

