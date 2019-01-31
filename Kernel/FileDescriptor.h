#pragma once

#include "VirtualFileSystem.h"
#include "InodeMetadata.h"
#include "FIFO.h"
#include <AK/ByteBuffer.h>
#include <AK/CircularQueue.h>
#include <AK/Retainable.h>
#include <AK/Badge.h>

class TTY;
class MasterPTY;
class Process;

class FileDescriptor : public Retainable<FileDescriptor> {
public:
    static RetainPtr<FileDescriptor> create(RetainPtr<Inode>&&);
    static RetainPtr<FileDescriptor> create(RetainPtr<CharacterDevice>&&);
    static RetainPtr<FileDescriptor> create_pipe_writer(FIFO&);
    static RetainPtr<FileDescriptor> create_pipe_reader(FIFO&);
    ~FileDescriptor();

    RetainPtr<FileDescriptor> clone();

    int close();

    off_t seek(off_t, int whence);
    ssize_t read(Process&, byte*, size_t);
    ssize_t write(Process&, const byte* data, size_t);
    int fstat(stat*);

    bool can_read(Process&);
    bool can_write(Process&);

    ssize_t get_dir_entries(byte* buffer, size_t);

    ByteBuffer read_entire_file(Process&);

    String absolute_path();

    bool is_directory() const;

    bool is_character_device() const { return m_device.ptr(); }
    CharacterDevice* character_device() { return m_device.ptr(); }
    const CharacterDevice* character_device() const { return m_device.ptr(); }

    bool is_tty() const;
    const TTY* tty() const;
    TTY* tty();

    bool is_master_pty() const;
    const MasterPTY* master_pty() const;
    MasterPTY* master_pty();

    InodeMetadata metadata() const;
    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }

    bool supports_mmap() const { return m_inode && !m_device; }

    bool is_blocking() const { return m_is_blocking; }
    void set_blocking(bool b) { m_is_blocking = b; }

    dword file_flags() const { return m_file_flags; }
    void set_file_flags(dword flags) { m_file_flags = flags; }

    bool is_fifo() const { return m_fifo; }
    FIFO::Direction fifo_direction() { return m_fifo_direction; }

    ByteBuffer& generator_cache() { return m_generator_cache; }

    void set_original_inode(Badge<VFS>, RetainPtr<Inode>&& inode) { m_inode = move(inode); }

private:
    friend class VFS;
    explicit FileDescriptor(RetainPtr<Inode>&&);
    explicit FileDescriptor(RetainPtr<CharacterDevice>&&);
    FileDescriptor(FIFO&, FIFO::Direction);

    RetainPtr<Inode> m_inode;
    RetainPtr<CharacterDevice> m_device;

    off_t m_current_offset { 0 };

    ByteBuffer m_generator_cache;

    bool m_is_blocking { true };
    dword m_file_flags { 0 };

    RetainPtr<FIFO> m_fifo;
    FIFO::Direction m_fifo_direction { FIFO::Neither };

    bool m_closed { false };
};

