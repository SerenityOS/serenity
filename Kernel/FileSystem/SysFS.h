/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Singleton.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/SystemExposed.h>
#include <Kernel/TTY/SlavePTY.h>

namespace Kernel {

class SysFS;
class SysFSInode;
class SysFSDirectoryInode;

class SysFSRootFolder final : public SystemExposedFolder {
    friend class SysFSComponentRegistry;

public:
    static NonnullRefPtr<SysFSRootFolder> create();
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;

private:
    SysFSRootFolder();
};

class SysFSComponentRegistry {
    friend class SysFS;
    friend class SysFSComponent;
    friend class SystemExposedFolder;
    friend class SysFSRootFolder;

public:
    static SysFSComponentRegistry& the();

    static void initialize();

    SysFSComponentRegistry();
    void register_new_component(SysFSComponent&);

    NonnullRefPtr<SystemExposedFolder> root_folder() { return m_root_folder; }

private:
    Lock m_lock;
    NonnullRefPtr<SysFSRootFolder> m_root_folder;
};

class SysFS final : public FileSystem {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static NonnullRefPtr<SysFS> create();

    virtual bool initialize() override;
    virtual char const* class_name() const override { return "SysFS"; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    SysFS();

    NonnullRefPtr<SysFSInode> m_root_inode;
};

class SysFSInode : public Inode {
    friend class SysFS;
    friend class SysFSDirectoryInode;

public:
    static NonnullRefPtr<SysFSInode> create(SysFS const&, SysFSComponent const&);
    StringView name() const { return m_associated_component->name(); }

protected:
    SysFSInode(SysFS const&, SysFSComponent const&);
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(String const& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, StringView const& name, mode_t) override;
    virtual KResult remove_child(StringView const& name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;

    NonnullRefPtr<SysFSComponent> m_associated_component;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;
    friend class SysFSRootDirectoryInode;

public:
    static NonnullRefPtr<SysFSDirectoryInode> create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSDirectoryInode() override;

protected:
    SysFSDirectoryInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;

    SysFS& m_parent_fs;
};

}
