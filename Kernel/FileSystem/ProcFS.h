#pragma once

#include <Kernel/Lock.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

class Process;

class ProcFSInode;

class ProcFS final : public FS {
    friend class ProcFSInode;
public:
    [[gnu::pure]] static ProcFS& the();

    virtual ~ProcFS() override;
    static Retained<ProcFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    virtual InodeIdentifier root_inode() const override;
    virtual RetainPtr<Inode> get_inode(InodeIdentifier) const override;

    virtual RetainPtr<Inode> create_inode(InodeIdentifier parent_id, const String& name, mode_t, off_t size, dev_t, int& error) override;
    virtual RetainPtr<Inode> create_directory(InodeIdentifier parent_id, const String& name, mode_t, int& error) override;

    void add_sys_file(String&&, Function<ByteBuffer(ProcFSInode&)>&& read_callback, Function<ssize_t(ProcFSInode&, const ByteBuffer&)>&& write_callback);
    void add_sys_bool(String&&, Lockable<bool>&, Function<void()>&& notify_callback = nullptr);
    void add_sys_string(String&&, Lockable<String>&, Function<void()>&& notify_callback = nullptr);

private:
    ProcFS();

    struct ProcFSDirectoryEntry {
        ProcFSDirectoryEntry() { }
        ProcFSDirectoryEntry(const char* a_name, unsigned a_proc_file_type, Function<ByteBuffer(InodeIdentifier)>&& a_read_callback = nullptr, Function<ssize_t(InodeIdentifier, const ByteBuffer&)>&& a_write_callback = nullptr, RetainPtr<ProcFSInode>&& a_inode = nullptr)
            : name(a_name)
            , proc_file_type(a_proc_file_type)
            , read_callback(move(a_read_callback))
            , write_callback(move(a_write_callback))
            , inode(move(a_inode))
        {
        }

        const char* name { nullptr };
        unsigned proc_file_type { 0 };
        Function<ByteBuffer(InodeIdentifier)> read_callback;
        Function<ssize_t(InodeIdentifier, const ByteBuffer&)> write_callback;
        RetainPtr<ProcFSInode> inode;
        InodeIdentifier identifier(unsigned fsid) const;
    };

    ProcFSDirectoryEntry* get_directory_entry(InodeIdentifier) const;

    Vector<ProcFSDirectoryEntry> m_entries;
    Vector<ProcFSDirectoryEntry> m_sys_entries;

    mutable Lock m_inodes_lock;
    mutable HashMap<unsigned, ProcFSInode*> m_inodes;
    RetainPtr<ProcFSInode> m_root_inode;
    Lockable<bool> m_kmalloc_stack_helper;
};

struct ProcFSInodeCustomData {
    virtual ~ProcFSInodeCustomData();
};

class ProcFSInode final : public Inode {
    friend class ProcFS;
public:
    virtual ~ProcFSInode() override;

    void set_custom_data(OwnPtr<ProcFSInodeCustomData>&& custom_data) { m_custom_data = move(custom_data); }
    ProcFSInodeCustomData* custom_data() { return m_custom_data.ptr(); }
    const ProcFSInodeCustomData* custom_data() const { return m_custom_data.ptr(); }

private:
    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, byte* buffer, FileDescriptor*) const override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const override;
    virtual InodeIdentifier lookup(const String& name) override;
    virtual String reverse_lookup(InodeIdentifier) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const byte* buffer, FileDescriptor*) override;
    virtual KResult add_child(InodeIdentifier child_id, const String& name, byte file_type) override;
    virtual KResult remove_child(const String& name) override;
    virtual RetainPtr<Inode> parent() const override;
    virtual size_t directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;

    ProcFS& fs() { return static_cast<ProcFS&>(Inode::fs()); }
    const ProcFS& fs() const { return static_cast<const ProcFS&>(Inode::fs()); }
    ProcFSInode(ProcFS&, unsigned index);

    OwnPtr<ProcFSInodeCustomData> m_custom_data;
};
