/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

namespace SegmentedProcFSIndex {
enum class MainProcessProperty {
    Reserved = 0,
    Unveil = 1,
    Pledge = 2,
    OpenFileDescriptions = 3,
    BinaryLink = 4,
    CurrentWorkDirectoryLink = 5,
    PerformanceEvents = 6,
    VirtualMemoryStats = 7,
    TTYLink = 8,
};

enum class ProcessSubDirectory {
    Reserved = 0,
    OpenFileDescriptions = 1,
    Stacks = 2,
};

void read_segments(u32& primary, ProcessSubDirectory& sub_directory, MainProcessProperty& property);
InodeIndex build_segmented_index_for_pid_directory(ProcessID);
InodeIndex build_segmented_index_for_sub_directory(ProcessID, ProcessSubDirectory sub_directory);
InodeIndex build_segmented_index_for_main_property(ProcessID, ProcessSubDirectory sub_directory, MainProcessProperty property);
InodeIndex build_segmented_index_for_main_property_in_pid_directory(ProcessID, MainProcessProperty property);
InodeIndex build_segmented_index_for_thread_stack(ProcessID, ThreadID);
InodeIndex build_segmented_index_for_file_description(ProcessID, unsigned);
}

class ProcFSComponentRegistry {
public:
    static ProcFSComponentRegistry& the();

    static void initialize();
    ProcFSComponentRegistry();

    ProcFSRootDirectory& root_directory() { return *m_root_directory; }
    Mutex& get_lock() { return m_lock; }

private:
    Mutex m_lock;
    NonnullRefPtr<ProcFSRootDirectory> m_root_directory;
};

class ProcFSExposedComponent : public RefCounted<ProcFSExposedComponent> {
public:
    StringView name() const { return m_name->view(); }
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const { VERIFY_NOT_REACHED(); }
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const { VERIFY_NOT_REACHED(); }
    virtual ErrorOr<NonnullRefPtr<ProcFSExposedComponent>> lookup(StringView) { VERIFY_NOT_REACHED(); };
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*) { return EROFS; }
    virtual ErrorOr<void> truncate(u64) { return EPERM; }
    virtual ErrorOr<void> set_mtime(time_t) { return ENOTIMPL; }

    virtual mode_t required_mode() const { return 0444; }
    virtual UserID owner_user() const { return 0; }
    virtual GroupID owner_group() const { return 0; }
    static time_t modified_time() { return TimeManagement::now().to_timeval().tv_sec; }

    virtual void prepare_for_deletion() { }
    virtual ErrorOr<void> refresh_data(OpenFileDescription&) const
    {
        return {};
    }

    virtual ErrorOr<NonnullRefPtr<Inode>> to_inode(const ProcFS& procfs_instance) const;

    virtual InodeIndex component_index() const { return m_component_index; }

    virtual ~ProcFSExposedComponent() = default;

protected:
    ProcFSExposedComponent();
    explicit ProcFSExposedComponent(StringView name);

private:
    OwnPtr<KString> m_name;
    InodeIndex m_component_index {};
};

class ProcFSExposedDirectory
    : public ProcFSExposedComponent
    , public Weakable<ProcFSExposedDirectory> {
    friend class ProcFSComponentRegistry;

public:
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<ProcFSExposedComponent>> lookup(StringView name) override;
    void add_component(const ProcFSExposedComponent&);

    virtual void prepare_for_deletion() override
    {
        for (auto& component : m_components) {
            component.prepare_for_deletion();
        }
    }
    virtual mode_t required_mode() const override { return 0555; }

    virtual ErrorOr<NonnullRefPtr<Inode>> to_inode(const ProcFS& procfs_instance) const override final;

protected:
    explicit ProcFSExposedDirectory(StringView name);
    ProcFSExposedDirectory(StringView name, const ProcFSExposedDirectory& parent_directory);
    NonnullRefPtrVector<ProcFSExposedComponent> m_components;
    WeakPtr<ProcFSExposedDirectory> m_parent_directory;
};

class ProcFSExposedLink : public ProcFSExposedComponent {
public:
    virtual ErrorOr<NonnullRefPtr<Inode>> to_inode(const ProcFS& procfs_instance) const override final;

    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const override;

protected:
    virtual bool acquire_link(KBufferBuilder& builder) = 0;
    explicit ProcFSExposedLink(StringView name);
    mutable Mutex m_lock { "ProcFSLink" };
};

class ProcFSRootDirectory final : public ProcFSExposedDirectory {
    friend class ProcFSComponentRegistry;

public:
    virtual ErrorOr<NonnullRefPtr<ProcFSExposedComponent>> lookup(StringView name) override;
    static NonnullRefPtr<ProcFSRootDirectory> must_create();
    virtual ~ProcFSRootDirectory();

private:
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    ProcFSRootDirectory();
};

struct ProcFSInodeData : public OpenFileDescriptionData {
    OwnPtr<KBuffer> buffer;
};

class ProcFSGlobalInformation : public ProcFSExposedComponent {
public:
    virtual ~ProcFSGlobalInformation() override {};

    virtual ErrorOr<size_t> read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const override;

    virtual mode_t required_mode() const override { return 0444; }

protected:
    explicit ProcFSGlobalInformation(StringView name)
        : ProcFSExposedComponent(name)
    {
    }
    virtual ErrorOr<void> refresh_data(OpenFileDescription&) const override;
    virtual ErrorOr<void> try_generate(KBufferBuilder&) = 0;

    mutable Mutex m_refresh_lock;
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

private:
    // ^ProcFSGlobalInformation
    virtual ErrorOr<void> try_generate(KBufferBuilder&) override final;

    // ^ProcFSExposedComponent
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*) override final;
    virtual mode_t required_mode() const override final { return 0644; }
    virtual ErrorOr<void> truncate(u64) override final;
    virtual ErrorOr<void> set_mtime(time_t) override final;
};

}
