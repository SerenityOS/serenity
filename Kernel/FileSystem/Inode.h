#pragma once

#include <AK/Retainable.h>
#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/WeakPtr.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/KResult.h>
#include <Kernel/Lock.h>

class FileDescriptor;
class LocalSocket;
class VMObject;

class Inode : public Retainable<Inode> {
    friend class VFS;
    friend class FS;
public:
    virtual ~Inode();

    virtual void one_retain_left() { }

    FS& fs() { return m_fs; }
    const FS& fs() const { return m_fs; }
    unsigned fsid() const;
    unsigned index() const { return m_index; }

    size_t size() const { return metadata().size; }
    bool is_symlink() const { return metadata().is_symlink(); }
    bool is_directory() const { return metadata().is_directory(); }
    bool is_character_device() const { return metadata().is_character_device(); }
    mode_t mode() const { return metadata().mode; }

    InodeIdentifier identifier() const { return { fsid(), index() }; }
    virtual InodeMetadata metadata() const = 0;

    ByteBuffer read_entire(FileDescriptor* = nullptr) const;

    virtual ssize_t read_bytes(off_t, ssize_t, byte* buffer, FileDescriptor*) const = 0;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const = 0;
    virtual InodeIdentifier lookup(const String& name) = 0;
    virtual String reverse_lookup(InodeIdentifier) = 0;
    virtual ssize_t write_bytes(off_t, ssize_t, const byte* data, FileDescriptor*) = 0;
    virtual KResult add_child(InodeIdentifier child_id, const String& name, byte file_type) = 0;
    virtual KResult remove_child(const String& name) = 0;
    virtual RetainPtr<Inode> parent() const = 0;
    virtual size_t directory_entry_count() const = 0;
    virtual KResult chmod(mode_t) = 0;
    virtual KResult chown(uid_t, gid_t) = 0;
    virtual KResult truncate(off_t) { return KSuccess; }

    LocalSocket* socket() { return m_socket.ptr(); }
    const LocalSocket* socket() const { return m_socket.ptr(); }
    bool bind_socket(LocalSocket&);
    bool unbind_socket();

    bool is_metadata_dirty() const { return m_metadata_dirty; }

    virtual int set_atime(time_t);
    virtual int set_ctime(time_t);
    virtual int set_mtime(time_t);
    virtual int increment_link_count();
    virtual int decrement_link_count();

    virtual void flush_metadata() = 0;

    void will_be_destroyed();

    void set_vmo(VMObject&);
    VMObject* vmo() { return m_vmo.ptr(); }
    const VMObject* vmo() const { return m_vmo.ptr(); }

    static void sync();

protected:
    Inode(FS& fs, unsigned index);
    void set_metadata_dirty(bool b) { m_metadata_dirty = b; }
    void inode_contents_changed(off_t, ssize_t, const byte*);
    void inode_size_changed(size_t old_size, size_t new_size);

    mutable Lock m_lock { "Inode" };

private:
    FS& m_fs;
    unsigned m_index { 0 };
    WeakPtr<VMObject> m_vmo;
    RetainPtr<LocalSocket> m_socket;
    bool m_metadata_dirty { false };
};

