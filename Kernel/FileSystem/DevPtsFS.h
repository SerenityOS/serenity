#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

class SlavePTY;
class DevPtsFSInode;

class DevPtsFS final : public FS {
public:
    virtual ~DevPtsFS() override;
    static NonnullRefPtr<DevPtsFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override { return "DevPtsFS"; }

    virtual InodeIdentifier root_inode() const override;
    virtual RefPtr<Inode> create_inode(InodeIdentifier parentInode, const String& name, mode_t, off_t size, dev_t, uid_t, gid_t, int& error) override;
    virtual RefPtr<Inode> create_directory(InodeIdentifier parentInode, const String& name, mode_t, uid_t, gid_t, int& error) override;
    virtual RefPtr<Inode> get_inode(InodeIdentifier) const override;

    static void register_slave_pty(SlavePTY&);
    static void unregister_slave_pty(SlavePTY&);

private:
    DevPtsFS();

    RefPtr<DevPtsFSInode> m_root_inode;
};

class DevPtsFSInode final : public Inode {
    friend class DevPtsFS;
public:
    virtual ~DevPtsFSInode() override;

private:
    DevPtsFSInode(DevPtsFS&, unsigned index);

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

    InodeMetadata m_metadata;
};
