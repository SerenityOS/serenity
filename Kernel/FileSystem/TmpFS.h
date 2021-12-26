#pragma once

#include <AK/Optional.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>

class TmpFSInode;

class TmpFS final : public FS {
    friend class TmpFSInode;

public:
    virtual ~TmpFS() override;
    static NonnullRefPtr<TmpFS> create();
    virtual bool initialize() override;

    virtual const char* class_name() const override { return "TmpFS"; }

    virtual bool supports_watchers() const override { return true; }

    virtual InodeIdentifier root_inode() const override;
    virtual RefPtr<Inode> get_inode(InodeIdentifier) const override;

    virtual RefPtr<Inode> create_inode(InodeIdentifier parent_id, const String& name, mode_t, off_t size, dev_t, int& error) override;
    virtual RefPtr<Inode> create_directory(InodeIdentifier parent_id, const String& name, mode_t, int& error) override;

private:
    TmpFS();

    RefPtr<TmpFSInode> m_root_inode;

    HashMap<unsigned, NonnullRefPtr<TmpFSInode>> m_inodes;
    void register_inode(TmpFSInode&);
    void unregister_inode(InodeIdentifier);

    unsigned m_next_inode_index { 1 };
    unsigned next_inode_index();
};

class TmpFSInode final : public Inode {
    friend class TmpFS;

public:
    virtual ~TmpFSInode() override;

    TmpFS& fs() { return static_cast<TmpFS&>(Inode::fs()); }
    const TmpFS& fs() const { return static_cast<const TmpFS&>(Inode::fs()); }

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
    virtual KResult truncate(off_t) override;
    virtual int set_atime(time_t) override;
    virtual int set_ctime(time_t) override;
    virtual int set_mtime(time_t) override;
    virtual void one_ref_left() override;

private:
    TmpFSInode(TmpFS& fs, InodeMetadata metadata, InodeIdentifier parent);
    static NonnullRefPtr<TmpFSInode> create(TmpFS&, InodeMetadata metadata, InodeIdentifier parent);
    static NonnullRefPtr<TmpFSInode> create_root(TmpFS&);

    InodeMetadata m_metadata;
    InodeIdentifier m_parent;

    Optional<KBuffer> m_content;
    struct Child {
        FS::DirectoryEntry entry;
        NonnullRefPtr<TmpFSInode> inode;
    };
    HashMap<String, Child> m_children;
};
