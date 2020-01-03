#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>

class Process;

class ProcFSInode;

class ProcFS final : public FS {
    friend class ProcFSInode;

public:
    virtual ~ProcFS() override;
    static NonnullRefPtr<ProcFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override;

    virtual InodeIdentifier root_inode() const override;
    virtual RefPtr<Inode> get_inode(InodeIdentifier) const override;

    virtual RefPtr<Inode> create_inode(InodeIdentifier parent_id, const String& name, mode_t, off_t size, dev_t, uid_t, gid_t, int& error) override;
    virtual RefPtr<Inode> create_directory(InodeIdentifier parent_id, const String& name, mode_t, uid_t, gid_t, int& error) override;

    static void add_sys_bool(String&&, Lockable<bool>&, Function<void()>&& notify_callback = nullptr);
    static void add_sys_string(String&&, Lockable<String>&, Function<void()>&& notify_callback = nullptr);

private:
    ProcFS();

    struct ProcFSDirectoryEntry {
        ProcFSDirectoryEntry() {}
        ProcFSDirectoryEntry(const char* a_name, unsigned a_proc_file_type, bool a_supervisor_only, Function<Optional<KBuffer>(InodeIdentifier)>&& a_read_callback = nullptr, Function<ssize_t(InodeIdentifier, const ByteBuffer&)>&& a_write_callback = nullptr, RefPtr<ProcFSInode>&& a_inode = nullptr)
            : name(a_name)
            , proc_file_type(a_proc_file_type)
            , supervisor_only(a_supervisor_only)
            , read_callback(move(a_read_callback))
            , write_callback(move(a_write_callback))
            , inode(move(a_inode))
        {
        }

        const char* name { nullptr };
        unsigned proc_file_type { 0 };
        bool supervisor_only { false };
        Function<Optional<KBuffer>(InodeIdentifier)> read_callback;
        Function<ssize_t(InodeIdentifier, const ByteBuffer&)> write_callback;
        RefPtr<ProcFSInode> inode;
        InodeIdentifier identifier(unsigned fsid) const;
    };

    ProcFSDirectoryEntry* get_directory_entry(InodeIdentifier) const;

    Vector<ProcFSDirectoryEntry> m_entries;

    mutable Lock m_inodes_lock;
    mutable HashMap<unsigned, ProcFSInode*> m_inodes;
    RefPtr<ProcFSInode> m_root_inode;
};

class ProcFSInode final : public Inode {
    friend class ProcFS;

public:
    virtual ~ProcFSInode() override;

private:
    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, u8* buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const override;
    virtual InodeIdentifier lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const u8* buffer, FileDescription*) override;
    virtual KResult add_child(InodeIdentifier child_id, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual size_t directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;

    ProcFS& fs() { return static_cast<ProcFS&>(Inode::fs()); }
    const ProcFS& fs() const { return static_cast<const ProcFS&>(Inode::fs()); }
    ProcFSInode(ProcFS&, unsigned index);
};
