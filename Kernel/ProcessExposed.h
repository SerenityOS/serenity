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

class ProcFS;
class ProcFSExposedComponent;
class ProcFSExposedFolder;
class ProcFSRootFolder;
class ProcFSBusDirectory;
class ProcFSSystemBoolean;

class ProcFSComponentsRegistrar {
    friend class ProcFS;
    friend class ProcFSExposedComponent;
    friend class ProcFSExposedFolder;
    friend class ProcFSRootFolder;

public:
    static ProcFSComponentsRegistrar& the();

    static void initialize();

    InodeIndex allocate_inode_index() const;

    ProcFSComponentsRegistrar();
    void register_new_bus_folder(ProcFSExposedFolder&);

    const ProcFSBusDirectory& buses_folder() const;

    void register_new_process(Process&);
    void unregister_process(Process&);

    ProcFSRootFolder& root_folder() { return *m_root_folder; }

private:
    Lock m_lock;
    NonnullRefPtr<ProcFSRootFolder> m_root_folder;
};

class ProcFSExposedComponent : public RefCounted<ProcFSExposedComponent> {
public:
    virtual KResultOr<size_t> entries_count() const { VERIFY_NOT_REACHED(); };
    StringView name() const { return m_name->view(); }
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const { VERIFY_NOT_REACHED(); }
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

class ProcFSExposedFolder
    : public ProcFSExposedComponent
    , public Weakable<ProcFSExposedFolder> {
    friend class ProcFSProcessFolder;
    friend class ProcFSComponentsRegistrar;

public:
    virtual KResultOr<size_t> entries_count() const override { return m_components.size(); };
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const override;
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
    explicit ProcFSExposedFolder(StringView name);
    ProcFSExposedFolder(StringView name, const ProcFSExposedFolder& parent_folder);
    NonnullRefPtrVector<ProcFSExposedComponent> m_components;
    WeakPtr<ProcFSExposedFolder> m_parent_folder;
};

class ProcFSExposedLink : public ProcFSExposedComponent {
public:
    virtual NonnullRefPtr<Inode> to_inode(const ProcFS& procfs_instance) const override final;

    virtual KResultOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const override;

protected:
    virtual bool acquire_link(KBufferBuilder& builder) = 0;
    explicit ProcFSExposedLink(StringView name);
    ProcFSExposedLink(StringView name, InodeIndex preallocated_index);
    mutable Lock m_lock { "ProcFSLink" };
};

class ProcFSRootFolder;
class ProcFSProcessInformation;

class ProcFSProcessFolder final
    : public ProcFSExposedFolder {
    friend class ProcFSComponentsRegistrar;
    friend class ProcFSRootFolder;
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
    static NonnullRefPtr<ProcFSProcessFolder> create(const Process&);
    NonnullRefPtr<Process> associated_process() { return m_associated_process; }

    virtual uid_t owner_user() const override { return m_associated_process->uid(); }
    virtual gid_t owner_group() const override { return m_associated_process->gid(); }
    virtual KResult refresh_data(FileDescription&) const override;
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

private:
    void on_attach();
    IntrusiveListNode<ProcFSProcessFolder, RefPtr<ProcFSProcessFolder>> m_list_node;

    explicit ProcFSProcessFolder(const Process&);
    NonnullRefPtr<Process> m_associated_process;
};

class ProcFSRootFolder;

class ProcFSBusDirectory : public ProcFSExposedFolder {
    friend class ProcFSComponentsRegistrar;

public:
    static NonnullRefPtr<ProcFSBusDirectory> must_create(const ProcFSRootFolder& parent_folder);

private:
    ProcFSBusDirectory(const ProcFSRootFolder& parent_folder);
};

class ProcFSRootFolder final : public ProcFSExposedFolder {
    friend class ProcFSComponentsRegistrar;

public:
    virtual RefPtr<ProcFSExposedComponent> lookup(StringView name) override;

    RefPtr<ProcFSProcessFolder> process_folder_for(Process&);
    static NonnullRefPtr<ProcFSRootFolder> must_create();
    virtual ~ProcFSRootFolder();

private:
    virtual KResult traverse_as_directory(unsigned, Function<bool(const FS::DirectoryEntryView&)>) const override;
    ProcFSRootFolder();

    RefPtr<ProcFSBusDirectory> m_buses_folder;
    IntrusiveList<ProcFSProcessFolder, RefPtr<ProcFSProcessFolder>, &ProcFSProcessFolder::m_list_node> m_process_folders;
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

    virtual uid_t owner_user() const override { return m_parent_folder.strong_ref()->m_associated_process->uid(); }
    virtual gid_t owner_group() const override { return m_parent_folder.strong_ref()->m_associated_process->gid(); }

protected:
    ProcFSProcessInformation(StringView name, const ProcFSProcessFolder& process_folder)
        : ProcFSExposedComponent(name)
        , m_parent_folder(process_folder)
    {
    }

    virtual KResult refresh_data(FileDescription&) const override;
    virtual bool output(KBufferBuilder& builder) = 0;

    WeakPtr<ProcFSProcessFolder> m_parent_folder;
    mutable SpinLock<u8> m_refresh_lock;
};

}
