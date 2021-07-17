/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/KResult.h>
#include <Kernel/Process.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class ProcFSComponentRegistry {
public:
    static ProcFSComponentRegistry& the();

    static void initialize();

    InodeIndex allocate_inode_index() const;

    ProcFSComponentRegistry();
    void register_new_bus_directory(ProcFSExposedDirectory&);

    const ProcFSBusDirectory& buses_directory() const;

    void register_new_process(Process&);
    void unregister_process(Process&);

    ProcFSRootDirectory& root_directory() { return *m_root_directory; }
    Mutex& get_lock() { return m_lock; }

private:
    Mutex m_lock;
    NonnullRefPtr<ProcFSRootDirectory> m_root_directory;
};

class ProcFSExposedComponent : public RefCounted<ProcFSExposedComponent> {
public:
    virtual KResultOr<size_t> entries_count() const { VERIFY_NOT_REACHED(); };
    StringView name() const { return m_name->view(); }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer&, FileDescription*) { return KResult(EROFS); }
    virtual size_t size() const { return 0; }

    virtual mode_t required_mode() const { return 0444; }
    virtual uid_t owner_user() const { return 0; }
    virtual gid_t owner_group() const { return 0; }
    time_t modified_time() const { return TimeManagement::now().to_timeval().tv_sec; }

    virtual void prepare_for_deletion() { }
    virtual KResult refresh_data(FileDescription&) const
    {
        return KSuccess;
    }

    virtual NonnullRefPtr<Inode> to_inode(const ProcFS& procfs_instance) const;

    InodeIndex component_index() const { return m_component_index; };

    virtual ~ProcFSExposedComponent() = default;

protected:
    explicit ProcFSExposedComponent(StringView name);
    ProcFSExposedComponent(StringView name, InodeIndex preallocated_index);

private:
    OwnPtr<KString> m_name;
    InodeIndex m_component_index {};
};

class ProcFSExposedDirectory
    : public ProcFSExposedComponent
    , public Weakable<ProcFSExposedDirectory> {
    friend class ProcFSProcessDirectory;
    friend class ProcFSComponentRegistry;

public:
    virtual KResultOr<size_t> entries_count() const override { return m_components.size(); };
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;
    void add_component(const ProcFSExposedComponent&);

    virtual void prepare_for_deletion() override
    {
        for (auto& component : m_components) {
            component.prepare_for_deletion();
        }
    }
    virtual mode_t required_mode() const override { return 0555; }

    virtual NonnullRefPtr<Inode> to_inode(const ProcFS& procfs_instance) const override final;

protected:
    explicit ProcFSExposedDirectory(StringView name);
    ProcFSExposedDirectory(StringView name, const ProcFSExposedDirectory& parent_directory);
    NonnullRefPtrVector<ProcFSExposedComponent> m_components;
    WeakPtr<ProcFSExposedDirectory> m_parent_directory;
};

class ProcFSExposedLink : public ProcFSExposedComponent {
public:
    virtual NonnullRefPtr<Inode> to_inode(const ProcFS& procfs_instance) const override final;

    virtual KResultOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const override;

protected:
    virtual bool acquire_link(KBufferBuilder& builder) = 0;
    explicit ProcFSExposedLink(StringView name);
    ProcFSExposedLink(StringView name, InodeIndex preallocated_index);
    mutable Mutex m_lock { "ProcFSLink" };
};

class ProcFSProcessDirectory final
    : public ProcFSExposedDirectory {
    friend class ProcFSComponentRegistry;
    friend class ProcFSRootDirectory;
    friend class ProcFSProcessInformation;
    friend class ProcFSProcessPledge;
    friend class ProcFSProcessUnveil;
    friend class ProcFSProcessPerformanceEvents;
    friend class ProcFSProcessFileDescription;
    friend class ProcFSProcessFileDescriptions;
    friend class ProcFSProcessOverallFileDescriptions;
    friend class ProcFSProcessRoot;
    friend class ProcFSProcessVirtualMemory;
    friend class ProcFSProcessCurrentWorkDirectory;
    friend class ProcFSProcessBinary;
    friend class ProcFSProcessStacks;

public:
    static NonnullRefPtr<ProcFSProcessDirectory> create(const Process&);
    RefPtr<Process> associated_process() { return m_associated_process; }

    virtual uid_t owner_user() const override { return m_associated_process ? m_associated_process->uid() : 0; }
    virtual gid_t owner_group() const override { return m_associated_process ? m_associated_process->gid() : 0; }
    virtual KResult refresh_data(FileDescription&) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    virtual void prepare_for_deletion() override;

private:
    void on_attach();
    IntrusiveListNode<ProcFSProcessDirectory, RefPtr<ProcFSProcessDirectory>> m_list_node;

    explicit ProcFSProcessDirectory(const Process&);
    RefPtr<Process> m_associated_process;
};

class ProcFSBusDirectory : public ProcFSExposedDirectory {
    friend class ProcFSComponentRegistry;

public:
    static NonnullRefPtr<ProcFSBusDirectory> must_create(const ProcFSRootDirectory& parent_directory);

private:
    ProcFSBusDirectory(const ProcFSRootDirectory& parent_directory);
};

class ProcFSRootDirectory final : public ProcFSExposedDirectory {
    friend class ProcFSComponentRegistry;

public:
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    RefPtr<ProcFSProcessDirectory> process_directory_for(Process&);
    static NonnullRefPtr<ProcFSRootDirectory> must_create();
    virtual ~ProcFSRootDirectory();

private:
    virtual KResult traverse_as_directory(unsigned, Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    ProcFSRootDirectory();

    RefPtr<ProcFSBusDirectory> m_buses_directory;
    IntrusiveList<ProcFSProcessDirectory, RefPtr<ProcFSProcessDirectory>, &ProcFSProcessDirectory::m_list_node> m_process_directories;
};

class ProcFSGlobalInformation : public ProcFSExposedComponent {
public:
    virtual ~ProcFSGlobalInformation() override {};

    virtual KResultOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const override;

    virtual mode_t required_mode() const override { return 0444; }

protected:
    explicit ProcFSGlobalInformation(StringView name)
        : ProcFSExposedComponent(name)
    {
    }
    virtual KResult refresh_data(FileDescription&) const override;
    virtual bool output(KBufferBuilder& builder) = 0;

    mutable SpinLock<u8> m_refresh_lock;
};

class ProcFSSystemBoolean : public ProcFSGlobalInformation {
public:
    virtual bool value() const = 0;
    virtual void set_value(bool new_value) = 0;

protected:
    explicit ProcFSSystemBoolean(StringView name)
        : ProcFSGlobalInformation(name)
    {
    }
    virtual bool output(KBufferBuilder& builder) override
    {
        builder.appendff("{}\n", value());
        return true;
    }
};

class ProcFSProcessInformation : public ProcFSExposedComponent {
public:
    virtual ~ProcFSProcessInformation() override {};

    virtual KResultOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const override;

    virtual uid_t owner_user() const override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (!parent_directory)
            return false;
        auto process = parent_directory->associated_process();
        if (!process)
            return false;
        return process->uid();
    }
    virtual gid_t owner_group() const override
    {
        auto parent_directory = m_parent_directory.strong_ref();
        if (!parent_directory)
            return false;
        auto process = parent_directory->associated_process();
        if (!process)
            return false;
        return process->gid();
    }

protected:
    ProcFSProcessInformation(StringView name, const ProcFSProcessDirectory& process_directory)
        : ProcFSExposedComponent(name)
        , m_parent_directory(process_directory)
    {
    }

    virtual KResult refresh_data(FileDescription&) const override;
    virtual bool output(KBufferBuilder& builder) = 0;

    WeakPtr<ProcFSProcessDirectory> m_parent_directory;
    mutable SpinLock<u8> m_refresh_lock;
};

}
