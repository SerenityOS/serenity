/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/SysFSComponent.h>

namespace Kernel {

class SysFSRootDirectory final : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    static NonnullRefPtr<SysFSRootDirectory> create();
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;

private:
    SysFSRootDirectory();
    RefPtr<SysFSBusDirectory> m_buses_directory;
};

class SysFSBusDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    static NonnullRefPtr<SysFSBusDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSBusDirectory(SysFSRootDirectory const&);
};

class SysFSComponentRegistry {
public:
    static SysFSComponentRegistry& the();

    static void initialize();

    SysFSComponentRegistry();
    void register_new_component(SysFSComponent&);

    SysFSDirectory& root_directory() { return m_root_directory; }
    Mutex& get_lock() { return m_lock; }

    void register_new_bus_directory(SysFSDirectory&);
    SysFSBusDirectory& buses_directory();

private:
    Mutex m_lock;
    NonnullRefPtr<SysFSRootDirectory> m_root_directory;
};

class SysFS final : public FileSystem {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static NonnullRefPtr<SysFS> create();

    virtual KResult initialize() override;
    virtual StringView class_name() const override { return "SysFS"sv; }

    virtual Inode& root_inode() override;

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
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult add_child(Inode&, StringView const& name, mode_t) override;
    virtual KResult remove_child(StringView const& name) override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(UserID, GroupID) override;
    virtual KResult truncate(u64) override;

    virtual KResult attach(FileDescription& description) override final;
    virtual void did_seek(FileDescription&, off_t) override final;

    NonnullRefPtr<SysFSComponent> m_associated_component;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;

public:
    static NonnullRefPtr<SysFSDirectoryInode> create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSDirectoryInode() override;

    SysFS& fs() { return static_cast<SysFS&>(Inode::fs()); }
    SysFS const& fs() const { return static_cast<SysFS const&>(Inode::fs()); }

protected:
    SysFSDirectoryInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

}
