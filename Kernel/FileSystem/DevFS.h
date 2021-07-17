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
    static NonnullRefPtr<DevFS> create();

    virtual bool initialize() override;
    virtual StringView class_name() const override { return "DevFS"sv; }

    void notify_new_device(Device&);
    void notify_device_removal(Device&);

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    DevFS();
    RefPtr<Inode> get_inode(InodeIdentifier) const;
    size_t allocate_inode_index();

    NonnullRefPtr<DevFSRootDirectoryInode> m_root_inode;
    NonnullRefPtrVector<DevFSInode> m_nodes;

    InodeIndex m_next_inode_index { 0 };
};

class DevFSInode : public Inode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual StringView name() const = 0;

protected:
    DevFSInode(DevFS&);
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;
};

class DevFSDeviceInode : public DevFSInode {
    friend class DevFS;
    friend class DevFSRootDirectoryInode;

public:
    virtual StringView name() const override;
    virtual ~DevFSDeviceInode() override;

private:
    String determine_name() const;
    DevFSDeviceInode(DevFS&, const Device&);
    // ^Inode
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResult chown(uid_t, gid_t) override;

    NonnullRefPtr<Device> m_attached_device;
    String m_cached_name;

    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
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
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, FileDescription*) override;

    NonnullOwnPtr<KString> m_name;
    String m_link;
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
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;

    NonnullRefPtrVector<DevFSDeviceInode> m_devices;
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
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> directory_entry_count() const override;
};

class DevFSRootDirectoryInode final : public DevFSDirectoryInode {
    friend class DevFS;

public:
    virtual ~DevFSRootDirectoryInode() override;
    virtual StringView name() const override { return "."; }

private:
    explicit DevFSRootDirectoryInode(DevFS&);
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> directory_entry_count() const override;

    NonnullRefPtrVector<DevFSDirectoryInode> m_subfolders;
    NonnullRefPtrVector<DevFSLinkInode> m_links;
    DevFS& m_parent_fs;
};

}
