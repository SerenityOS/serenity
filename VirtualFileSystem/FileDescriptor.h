#pragma once

#include "VirtualFileSystem.h"
#include "InodeMetadata.h"
#include "FIFO.h"
#include <AK/ByteBuffer.h>
#include <AK/CircularQueue.h>
#include <AK/Retainable.h>

#ifdef SERENITY
class TTY;
class MasterPTY;
class Process;
#endif

class FileDescriptor : public Retainable<FileDescriptor> {
public:
    static RetainPtr<FileDescriptor> create(RetainPtr<Vnode>&&);
    static RetainPtr<FileDescriptor> create_pipe_writer(FIFO&);
    static RetainPtr<FileDescriptor> create_pipe_reader(FIFO&);
    ~FileDescriptor();

    RetainPtr<FileDescriptor> clone();

    int close();

    Unix::off_t seek(Unix::off_t, int whence);
    ssize_t read(Process&, byte*, size_t);
    ssize_t write(Process&, const byte* data, size_t);
    int stat(Unix::stat*);

    bool can_read(Process&);
    bool can_write(Process&);

    ssize_t get_dir_entries(byte* buffer, size_t);

    ByteBuffer read_entire_file(Process&);

    String absolute_path();

    bool is_directory() const;

    bool is_character_device() const { return m_vnode && m_vnode->isCharacterDevice(); }
    CharacterDevice* character_device() { return m_vnode ? m_vnode->characterDevice() : nullptr; }

#ifdef SERENITY
    bool is_tty() const;
    const TTY* tty() const;
    TTY* tty();

    bool is_master_pty() const;
    const MasterPTY* master_pty() const;
    MasterPTY* master_pty();
#endif

    InodeMetadata metadata() const { return m_vnode->metadata(); }

    Vnode* vnode() { return m_vnode.ptr(); }
    Inode* inode() { return m_vnode ? m_vnode->core_inode() : nullptr; }

#ifdef SERENITY
    bool is_blocking() const { return m_is_blocking; }
    void set_blocking(bool b) { m_is_blocking = b; }

    dword file_flags() const { return m_file_flags; }
    void set_file_flags(dword flags) { m_file_flags = flags; }

    bool is_fifo() const { return m_fifo; }
    FIFO::Direction fifo_direction() { return m_fifo_direction; }
#endif

    ByteBuffer& generator_cache() { return m_generator_cache; }

private:
    friend class VFS;
    explicit FileDescriptor(RetainPtr<Vnode>&&);
    FileDescriptor(FIFO&, FIFO::Direction);

    RetainPtr<Vnode> m_vnode;

    Unix::off_t m_current_offset { 0 };

    ByteBuffer m_generator_cache;

#ifdef SERENITY
    bool m_is_blocking { true };
    dword m_file_flags { 0 };

    RetainPtr<FIFO> m_fifo;
    FIFO::Direction m_fifo_direction { FIFO::Neither };
#endif
};

