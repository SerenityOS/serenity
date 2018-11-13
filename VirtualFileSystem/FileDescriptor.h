#pragma once

#include "VirtualFileSystem.h"
#include "InodeMetadata.h"
#include "FIFO.h"
#include <AK/ByteBuffer.h>
#include <AK/CircularQueue.h>
#include <AK/Retainable.h>

#ifdef SERENITY
class TTY;
#endif

class FileDescriptor : public Retainable<FileDescriptor> {
public:
    static RetainPtr<FileDescriptor> create(RetainPtr<VirtualFileSystem::Node>&&);
    static RetainPtr<FileDescriptor> create_pipe_writer(FIFO&);
    static RetainPtr<FileDescriptor> create_pipe_reader(FIFO&);
    ~FileDescriptor();

    RetainPtr<FileDescriptor> clone();

    int close();

    Unix::off_t seek(Unix::off_t, int whence);
    Unix::ssize_t read(byte*, Unix::size_t);
    Unix::ssize_t write(const byte* data, Unix::size_t);
    int stat(Unix::stat*);

    bool hasDataAvailableForRead();
    bool can_write();

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
    void set_file_flags(dword flags) { m_file_flags = flags; }

    bool is_fifo() const { return m_fifo; }
    FIFO::Direction fifo_direction() { return m_fifo_direction; }
#endif

    ByteBuffer& generatorCache() { return m_generatorCache; }

private:
    friend class VirtualFileSystem;
    explicit FileDescriptor(RetainPtr<VirtualFileSystem::Node>&&);
    FileDescriptor(FIFO&, FIFO::Direction);

    RetainPtr<VirtualFileSystem::Node> m_vnode;
    RetainPtr<CoreInode> m_inode;

    Unix::off_t m_currentOffset { 0 };

    ByteBuffer m_generatorCache;

#ifdef SERENITY
    bool m_isBlocking { true };
    dword m_file_flags { 0 };

    RetainPtr<FIFO> m_fifo;
    FIFO::Direction m_fifo_direction { FIFO::Neither };
#endif
};

