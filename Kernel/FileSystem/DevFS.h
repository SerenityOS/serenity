/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

class DevFS final : public FileSystem {
    friend class DevFSInode;
    friend class DevFSRootDirectoryInode;

public:
    virtual ~DevFS() override;
    static KResultOr<NonnullRefPtr<DevFS>> try_create();

    virtual KResult initialize() override;
    virtual StringView class_name() const override { return "DevFS"sv; }

    void notify_new_device(Device&);
    void notify_device_removal(Device&);

    virtual Inode& root_inode() override;

private:
    DevFS();
    size_t allocate_inode_index();

    RefPtr<DevFSRootDirectoryInode> m_root_inode;
    InodeIndex m_next_inode_index { 0 };
};

class DevFSInode : public Inode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;
    friend class DevFSDirectoryInode;

public:
    virtual StringView name() const = 0;

    DevFS& fs() { return static_cast<DevFS&>(Inode::fs()); }
    DevFS const& fs() const { return static_cast<DevFS const&>(Inode::fs()); }

protected:
    DevFSInode(DevFS&);
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(UserID, GroupID) override;
    virtual KResult truncate(u64) override;

private:
    IntrusiveListNode<DevFSInode, RefPtr<DevFSInode>> m_list_node;
};

class DevFSDeviceInode : public DevFSInode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual StringView name() const override;
    virtual ~DevFSDeviceInode() override;

private:
    DevFSDeviceInode(DevFS&, Device const&, NonnullOwnPtr<KString> name);
    // ^Inode
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;
    virtual KResult chown(UserID, GroupID) override;

    NonnullRefPtr<Device> m_attached_device;
    NonnullOwnPtr<KString> m_name;

    UserID m_uid { 0 };
    GroupID m_gid { 0 };
};

class DevFSLinkInode : public DevFSInode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual StringView name() const override;
    virtual ~DevFSLinkInode() override;

protected:
    DevFSLinkInode(DevFS&, NonnullOwnPtr<KString>);
    // ^Inode
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;

    NonnullOwnPtr<KString> m_name;
    OwnPtr<KString> m_link;
};

class DevFSDirectoryInode : public DevFSInode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual ~DevFSDirectoryInode() override;

protected:
    DevFSDirectoryInode(DevFS&);
    // ^Inode
    virtual InodeMetadata metadata() const override;

    IntrusiveList<DevFSInode, NonnullRefPtr<DevFSInode>, &DevFSInode::m_list_node> m_nodes;
};

class DevFSPtsDirectoryInode final : public DevFSDirectoryInode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual ~DevFSPtsDirectoryInode() override;
    virtual StringView name() const override { return "pts"; };

private:
    explicit DevFSPtsDirectoryInode(DevFS&);
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual InodeMetadata metadata() const override;
};

class DevFSRootDirectoryInode final : public DevFSDirectoryInode {
    friend class DevFS;

public:
    virtual ~DevFSRootDirectoryInode() override;
    virtual StringView name() const override { return "."; }

private:
    explicit DevFSRootDirectoryInode(DevFS&);
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual InodeMetadata metadata() const override;
};

}
