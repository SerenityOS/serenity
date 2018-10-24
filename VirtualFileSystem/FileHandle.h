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

    ssize_t get_dir_entries(byte* buffer, Unix::size_t);

    ByteBuffer readEntireFile();

#ifdef SERENITY
    int fd() const { return m_fd; }
    void setFD(int fd) { m_fd = fd; }
#endif

private:
    friend class VirtualFileSystem;

    RetainPtr<VirtualFileSystem::Node> m_vnode;

    Unix::off_t m_currentOffset { 0 };

#ifdef SERENITY
    int m_fd { -1 };
#endif
};

