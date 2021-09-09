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
    static NonnullRefPtr<SysFSRootDirectory> create();
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;

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

    Device const& device() const { return *m_associated_device; }

private:
    explicit SysFSDeviceComponent(Device const&);
    IntrusiveListNode<SysFSDeviceComponent, NonnullRefPtr<SysFSDeviceComponent>> m_list_node;
    RefPtr<Device> m_associated_device;
};

class SysFSDevicesDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSDevicesDirectory> must_create(SysFSRootDirectory const&);

private:
    explicit SysFSDevicesDirectory(SysFSRootDirectory const&);
};

class SysFSBlockDevicesDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSBlockDevicesDirectory> must_create(SysFSDevicesDirectory const&);
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSBlockDevicesDirectory(SysFSDevicesDirectory const&);
};

class SysFSCharacterDevicesDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<SysFSCharacterDevicesDirectory> must_create(SysFSDevicesDirectory const&);
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

private:
    explicit SysFSCharacterDevicesDirectory(SysFSDevicesDirectory const&);
};

class SysFSBusDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
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
    static KResultOr<NonnullRefPtr<SysFS>> try_create();

    virtual KResult initialize() override;
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
    static KResultOr<NonnullRefPtr<SysFSInode>> try_create(SysFS const&, SysFSComponent const&);
    StringView name() const { return m_associated_component->name(); }

protected:
    SysFSInode(SysFS const&, SysFSComponent const&);
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult add_child(Inode&, StringView const& name, mode_t) override;
    virtual KResult remove_child(StringView const& name) override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(UserID, GroupID) override;
    virtual KResult truncate(u64) override;

    virtual KResult attach(OpenFileDescription& description) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;

    NonnullRefPtr<SysFSComponent> m_associated_component;
};

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;

public:
    static KResultOr<NonnullRefPtr<SysFSDirectoryInode>> try_create(SysFS const&, SysFSComponent const&);
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
