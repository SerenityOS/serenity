#pragma once

#include "VirtualFileSystem.h"
#include "InodeMetadata.h"
#include <AK/ByteBuffer.h>
#include <AK/Retainable.h>

class TTY;

class FileHandle : public Retainable<FileHandle> {
public:
    static RetainPtr<FileHandle> create(RetainPtr<VirtualFileSystem::Node>&&);
    ~FileHandle();

    RetainPtr<FileHandle> clone();

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

    bool isTTY() const;
    const TTY* tty() const;
    TTY* tty();

    InodeMetadata metadata() const { return m_vnode->metadata(); }

    VirtualFileSystem::Node* vnode() { return m_vnode.ptr(); }

#ifdef SERENITY
    bool isBlocking() const { return m_isBlocking; }
    void setBlocking(bool b) { m_isBlocking = b; }
#endif

    ByteBuffer& generatorCache() { return m_generatorCache; }

private:
    friend class VirtualFileSystem;
    explicit FileHandle(RetainPtr<VirtualFileSystem::Node>&&);

    RetainPtr<VirtualFileSystem::Node> m_vnode;

    Unix::off_t m_currentOffset { 0 };

    ByteBuffer m_generatorCache;

#ifdef SERENITY
    int m_fd { -1 };
    bool m_isBlocking { true };
#endif
};

