/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/SysFSComponent.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class SysFSDevicesDirectory;
class SysFSRootDirectory final : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "."sv; }
    static NonnullRefPtr<SysFSRootDirectory> create();
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;

private:
    SysFSRootDirectory();
    RefPtr<SysFSBusDirectory> m_buses_directory;
};

class SysFSDeviceComponent final
    : public SysFSComponent
    , public Weakable<SysFSDeviceComponent> {
    friend class SysFSComponentRegistry;

public:
    static NonnullRefPtr<SysFSDeviceComponent> must_create(Device const&);
    virtual StringView name() const override { return m_major_minor_formatted_device_name->view(); }
    bool is_block_device() const { return m_block_device; }

private:
    SysFSDeviceComponent(NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const&);
    IntrusiveListNode<SysFSDeviceComponent, NonnullRefPtr<SysFSDeviceComponent>> m_list_node;
    bool m_block_device;

    NonnullOwnPtr<KString> m_major_minor_formatted_device_name;
};

class SysFSDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "dev"sv; }
    static NonnullRefPtr<SysFSDevicesDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSDevicesDirectory(SysFSRootDirectory const&);
};

class SysFSBlockDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "block"sv; }
    static NonnullRefPtr<SysFSBlockDevicesDirectory> must_create(SysFSDevicesDirectory const&);
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSBlockDevicesDirectory(SysFSDevicesDirectory const&);
};

class SysFSCharacterDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "char"sv; }
    static NonnullRefPtr<SysFSCharacterDevicesDirectory> must_create(SysFSDevicesDirectory const&);
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSCharacterDevicesDirectory(SysFSDevicesDirectory const&);
};

class SysFSBusDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "bus"sv; }
    static NonnullRefPtr<SysFSBusDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSBusDirectory(SysFSRootDirectory const&);
};

class SysFSComponentRegistry {
    using DevicesList = MutexProtected<IntrusiveList<&SysFSDeviceComponent::m_list_node>>;

public:
    static SysFSComponentRegistry& the();

    static void initialize();

    SysFSComponentRegistry();
    void register_new_component(SysFSComponent&);

    SysFSDirectory& root_directory() { return m_root_directory; }
    Mutex& get_lock() { return m_lock; }

    void register_new_bus_directory(SysFSDirectory&);
    SysFSBusDirectory& buses_directory();

    DevicesList& devices_list();

private:
    Mutex m_lock;
    NonnullRefPtr<SysFSRootDirectory> m_root_directory;
    DevicesList m_devices_list;
};

class SysFS final : public FileSystem {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static ErrorOr<NonnullRefPtr<SysFS>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "SysFS"sv; }

    virtual Inode& root_inode() override;

private:
    SysFS();

    RefPtr<SysFSInode> m_root_inode;
};

class SysFSInode : public Inode {
    friend class SysFS;
    friend class SysFSDirectoryInode;

public:
    static ErrorOr<NonnullRefPtr<SysFSInode>> try_create(SysFS const&, SysFSComponent const&);
    StringView name() const { return m_associated_component->name(); }

protected:
    SysFSInode(SysFS const&, SysFSComponent const&);
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> set_mtime(time_t) override;

    virtual ErrorOr<void> attach(OpenFileDescription& description) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;

    NonnullRefPtr<SysFSComponent> m_associated_component;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;

public:
    static ErrorOr<NonnullRefPtr<SysFSDirectoryInode>> try_create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSDirectoryInode() override;

    SysFS& fs() { return static_cast<SysFS&>(Inode::fs()); }
    SysFS const& fs() const { return static_cast<SysFS const&>(Inode::fs()); }

protected:
    SysFSDirectoryInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

}
