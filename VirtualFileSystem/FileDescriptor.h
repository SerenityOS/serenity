#pragma once

#include "VirtualFileSystem.h"
#include "InodeMetadata.h"
#include <AK/ByteBuffer.h>
#include <AK/Retainable.h>

#ifdef SERENITY
class TTY;
#endif

class FileDescriptor : public Retainable<FileDescriptor> {
public:
    static RetainPtr<FileDescriptor> create(RetainPtr<VirtualFileSystem::Node>&&);
    ~FileDescriptor();

    RetainPtr<FileDescriptor> clone();

    int close();

    Unix::off_t seek(Unix::off_t, int whence);
    Unix::ssize_t read(byte*, Unix::size_t);
    Unix::ssize_t write(const byte* data, Unix::size_t);
    int stat(Unix::stat*);

    bool hasDataAvailableForRead();

    ssize_t get_dir_entries(byte* buffer, Unix::size_t);

    ByteBuffer readEntireFile();

    String absolute_path() const;

    bool isDirectory() const;

#ifdef SERENITY
    bool isTTY() const;
    const TTY* tty() const;
    TTY* tty();
#endif

    InodeMetadata metadata() const { return m_vnode->metadata(); }

    VirtualFileSystem::Node* vnode() { return m_vnode.ptr(); }

#ifdef SERENITY
    bool isBlocking() const { return m_isBlocking; }
    void setBlocking(bool b) { m_isBlocking = b; }

    dword file_flags() const { return m_file_flags; }
    int set_file_flags(dword flags) { m_file_flags = flags; return 0; }

    dword fd_flags() const { return m_fd_flags; }
    int set_fd_flags(dword flags) { m_fd_flags = flags; return 0; }
#endif

    ByteBuffer& generatorCache() { return m_generatorCache; }

private:
    friend class VirtualFileSystem;
    explicit FileDescriptor(RetainPtr<VirtualFileSystem::Node>&&);

    RetainPtr<VirtualFileSystem::Node> m_vnode;

    Unix::off_t m_currentOffset { 0 };

    ByteBuffer m_generatorCache;

#ifdef SERENITY
    bool m_isBlocking { true };
    dword m_fd_flags { 0 };
    dword m_file_flags { 0 };
#endif
};

