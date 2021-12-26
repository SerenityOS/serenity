#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/CircularQueue.h>
#include <AK/RefCounted.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/VM/VirtualAddress.h>

class File;
class TTY;
class MasterPTY;
class Process;
class Region;
class CharacterDevice;
class SharedMemory;

class FileDescription : public RefCounted<FileDescription> {
public:
    static NonnullRefPtr<FileDescription> create(RefPtr<Custody>&&);
    static NonnullRefPtr<FileDescription> create(RefPtr<File>&&, SocketRole = SocketRole::None);
    ~FileDescription();

    NonnullRefPtr<FileDescription> clone();

    int close();

    off_t seek(off_t, int whence);
    ssize_t read(u8*, ssize_t);
    ssize_t write(const u8* data, ssize_t);
    KResult fstat(stat&);

    KResult fchmod(mode_t);

    bool can_read();
    bool can_write();

    ssize_t get_dir_entries(u8* buffer, ssize_t);

    ByteBuffer read_entire_file();

    String absolute_path() const;

    bool is_directory() const;

    File& file() { return *m_file; }
    const File& file() const { return *m_file; }

    bool is_device() const;

    bool is_tty() const;
    const TTY* tty() const;
    TTY* tty();

    bool is_master_pty() const;
    const MasterPTY* master_pty() const;
    MasterPTY* master_pty();

    InodeMetadata metadata() const;
    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }

    Custody* custody() { return m_custody.ptr(); }
    const Custody* custody() const { return m_custody.ptr(); }

    KResultOr<Region*> mmap(Process&, VirtualAddress, size_t offset, size_t, int prot);

    bool is_blocking() const { return m_is_blocking; }
    void set_blocking(bool b) { m_is_blocking = b; }
    bool should_append() const { return m_should_append; }
    void set_should_append(bool s) { m_should_append = s; }

    u32 file_flags() const { return m_file_flags; }
    void set_file_flags(u32);

    bool is_socket() const;
    Socket* socket();
    const Socket* socket() const;

    bool is_fifo() const;
    FIFO* fifo();
    FIFO::Direction fifo_direction() { return m_fifo_direction; }
    void set_fifo_direction(Badge<FIFO>, FIFO::Direction direction) { m_fifo_direction = direction; }

    bool is_shared_memory() const;
    SharedMemory* shared_memory();
    const SharedMemory* shared_memory() const;

    ByteBuffer& generator_cache() { return m_generator_cache; }

    void set_original_inode(Badge<VFS>, NonnullRefPtr<Inode>&& inode) { m_inode = move(inode); }

    SocketRole socket_role() const { return m_socket_role; }
    void set_socket_role(SocketRole);

    KResult truncate(off_t);

    off_t offset() const { return m_current_offset; }

    KResult chown(uid_t, gid_t);

private:
    friend class VFS;
    FileDescription(RefPtr<File>&&, SocketRole = SocketRole::None);
    FileDescription(FIFO&, FIFO::Direction);

    RefPtr<Custody> m_custody;
    RefPtr<Inode> m_inode;
    RefPtr<File> m_file;

    off_t m_current_offset { 0 };

    ByteBuffer m_generator_cache;

    u32 m_file_flags { 0 };

    bool m_is_blocking { true };
    bool m_should_append { false };
    SocketRole m_socket_role { SocketRole::None };
    FIFO::Direction m_fifo_direction { FIFO::Direction::Neither };
};
