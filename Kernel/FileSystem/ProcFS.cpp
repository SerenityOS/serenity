/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<ProcFSComponentRegistry> s_the;

ProcFSComponentRegistry& ProcFSComponentRegistry::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT void ProcFSComponentRegistry::initialize()
{
    VERIFY(!s_the.is_initialized());
    s_the.ensure_instance();
}

UNMAP_AFTER_INIT ProcFSComponentRegistry::ProcFSComponentRegistry()
    : m_root_directory(ProcFSRootDirectory::must_create())
{
}

ErrorOr<NonnullRefPtr<ProcFS>> ProcFS::try_create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFS());
}

ProcFS::ProcFS()
{
}

ProcFS::~ProcFS()
{
}

ErrorOr<void> ProcFS::initialize()
{
    m_root_inode = static_ptr_cast<ProcFSDirectoryInode>(TRY(ProcFSComponentRegistry::the().root_directory().to_inode(*this)));
    return {};
}

Inode& ProcFS::root_inode()
{
    return *m_root_inode;
}

ProcFSInode::ProcFSInode(const ProcFS& fs, InodeIndex index)
    : Inode(const_cast<ProcFS&>(fs), index)
{
}

ProcFSInode::~ProcFSInode()
{
}

ErrorOr<void> ProcFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> ProcFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::remove_child(StringView)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::chmod(mode_t)
{
    return EPERM;
}

ErrorOr<void> ProcFSInode::chown(UserID, GroupID)
{
    return EPERM;
}

ErrorOr<NonnullRefPtr<ProcFSGlobalInode>> ProcFSGlobalInode::try_create(const ProcFS& fs, const ProcFSExposedComponent& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSGlobalInode(fs, component));
}

ProcFSGlobalInode::ProcFSGlobalInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : ProcFSInode(fs, component.component_index())
    , m_associated_component(component)
{
}

void ProcFSGlobalInode::did_seek(OpenFileDescription& description, off_t new_offset)
{
    if (new_offset != 0)
        return;
    auto result = m_associated_component->refresh_data(description);
    if (result.is_error()) {
        // Subsequent calls to read will return EIO!
        dbgln("ProcFS: Could not refresh contents: {}", result.error());
    }
}

ErrorOr<void> ProcFSGlobalInode::attach(OpenFileDescription& description)
{
    return m_associated_component->refresh_data(description);
}

ErrorOr<size_t> ProcFSGlobalInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* fd) const
{
    return m_associated_component->read_bytes(offset, count, buffer, fd);
}

StringView ProcFSGlobalInode::name() const
{
    return m_associated_component->name();
}

ErrorOr<void> ProcFSGlobalInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSGlobalInode::lookup(StringView)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSGlobalInode::truncate(u64 size)
{
    return m_associated_component->truncate(size);
}

ErrorOr<void> ProcFSGlobalInode::set_mtime(time_t time)
{
    return m_associated_component->set_mtime(time);
}

InodeMetadata ProcFSGlobalInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFREG | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

ErrorOr<size_t> ProcFSGlobalInode::write_bytes(off_t offset, size_t count, const UserOrKernelBuffer& buffer, OpenFileDescription* fd)
{
    return m_associated_component->write_bytes(offset, count, buffer, fd);
}

ErrorOr<NonnullRefPtr<ProcFSDirectoryInode>> ProcFSDirectoryInode::try_create(const ProcFS& procfs, const ProcFSExposedComponent& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSDirectoryInode(procfs, component));
}

ProcFSDirectoryInode::ProcFSDirectoryInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : ProcFSGlobalInode(fs, component)
{
}

ProcFSDirectoryInode::~ProcFSDirectoryInode()
{
}
InodeMetadata ProcFSDirectoryInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFDIR | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}
ErrorOr<void> ProcFSDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    return m_associated_component->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto component = TRY(m_associated_component->lookup(name));
    return component->to_inode(procfs());
}

ErrorOr<NonnullRefPtr<ProcFSLinkInode>> ProcFSLinkInode::try_create(const ProcFS& procfs, const ProcFSExposedComponent& component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSLinkInode(procfs, component));
}

ProcFSLinkInode::ProcFSLinkInode(const ProcFS& fs, const ProcFSExposedComponent& component)
    : ProcFSGlobalInode(fs, component)
{
}

InodeMetadata ProcFSLinkInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = { fsid(), m_associated_component->component_index() };
    metadata.mode = S_IFLNK | m_associated_component->required_mode();
    metadata.uid = m_associated_component->owner_user();
    metadata.gid = m_associated_component->owner_group();
    metadata.size = 0;
    metadata.mtime = m_associated_component->modified_time();
    return metadata;
}

ProcFSProcessAssociatedInode::ProcFSProcessAssociatedInode(const ProcFS& fs, ProcessID associated_pid, InodeIndex determined_index)
    : ProcFSInode(fs, determined_index)
    , m_pid(associated_pid)
{
}

ErrorOr<size_t> ProcFSProcessAssociatedInode::write_bytes(off_t, size_t, const UserOrKernelBuffer&, OpenFileDescription*)
{
    return ENOTSUP;
}

ErrorOr<NonnullRefPtr<ProcFSProcessDirectoryInode>> ProcFSProcessDirectoryInode::try_create(const ProcFS& procfs, ProcessID pid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSProcessDirectoryInode(procfs, pid));
}

ProcFSProcessDirectoryInode::ProcFSProcessDirectoryInode(const ProcFS& procfs, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(pid))
{
}

ErrorOr<void> ProcFSProcessDirectoryInode::attach(OpenFileDescription&)
{
    return {};
}

InodeMetadata ProcFSProcessDirectoryInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return {};

    auto traits = process->procfs_traits();
    InodeMetadata metadata;
    metadata.inode = { fsid(), traits->component_index() };
    metadata.mode = S_IFDIR | traits->required_mode();
    metadata.uid = traits->owner_user();
    metadata.gid = traits->owner_group();
    metadata.size = 0;
    metadata.mtime = traits->modified_time();
    return metadata;
}

ErrorOr<size_t> ProcFSProcessDirectoryInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSProcessDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return EINVAL;
    return process->procfs_traits()->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSProcessDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return ESRCH;
    if (name == "fd"sv)
        return TRY(ProcFSProcessSubDirectoryInode::try_create(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions, associated_pid()));
    if (name == "stacks"sv)
        return TRY(ProcFSProcessSubDirectoryInode::try_create(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks, associated_pid()));
    if (name == "unveil"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::Unveil, associated_pid()));
    if (name == "pledge"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::Pledge, associated_pid()));
    if (name == "fds"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions, associated_pid()));
    if (name == "exe"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink, associated_pid()));
    if (name == "cwd"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink, associated_pid()));
    if (name == "perf_events"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents, associated_pid()));
    if (name == "vm"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats, associated_pid()));
    if (name == "tty"sv)
        return TRY(ProcFSProcessPropertyInode::try_create_for_pid_property(procfs(), SegmentedProcFSIndex::MainProcessProperty::TTYLink, associated_pid()));
    return ENOENT;
}

ErrorOr<NonnullRefPtr<ProcFSProcessSubDirectoryInode>> ProcFSProcessSubDirectoryInode::try_create(const ProcFS& procfs, SegmentedProcFSIndex::ProcessSubDirectory sub_directory_type, ProcessID pid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSProcessSubDirectoryInode(procfs, sub_directory_type, pid));
}

ProcFSProcessSubDirectoryInode::ProcFSProcessSubDirectoryInode(const ProcFS& procfs, SegmentedProcFSIndex::ProcessSubDirectory sub_directory_type, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(pid, sub_directory_type))
    , m_sub_directory_type(sub_directory_type)
{
}

ErrorOr<size_t> ProcFSProcessSubDirectoryInode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> ProcFSProcessSubDirectoryInode::attach(OpenFileDescription&)
{
    return {};
}

void ProcFSProcessSubDirectoryInode::did_seek(OpenFileDescription&, off_t)
{
    VERIFY_NOT_REACHED();
}

InodeMetadata ProcFSProcessSubDirectoryInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return {};

    auto traits = process->procfs_traits();
    InodeMetadata metadata;
    metadata.inode = { fsid(), traits->component_index() };
    metadata.mode = S_IFDIR | traits->required_mode();
    metadata.uid = traits->owner_user();
    metadata.gid = traits->owner_group();
    metadata.size = 0;
    metadata.mtime = traits->modified_time();
    return metadata;
}

ErrorOr<void> ProcFSProcessSubDirectoryInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return EINVAL;
    switch (m_sub_directory_type) {
    case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
        return process->traverse_file_descriptions_directory(procfs().fsid(), move(callback));
    case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
        return process->traverse_stacks_directory(procfs().fsid(), move(callback));
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSProcessSubDirectoryInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return ESRCH;
    switch (m_sub_directory_type) {
    case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
        return process->lookup_file_descriptions_directory(procfs(), name);
    case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
        return process->lookup_stacks_directory(procfs(), name);
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_file_description_link(const ProcFS& procfs, unsigned file_description_index, ProcessID pid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, file_description_index, pid));
}
ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_thread_stack(const ProcFS& procfs, ThreadID stack_thread_index, ProcessID pid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, stack_thread_index, pid));
}
ErrorOr<NonnullRefPtr<ProcFSProcessPropertyInode>> ProcFSProcessPropertyInode::try_create_for_pid_property(const ProcFS& procfs, SegmentedProcFSIndex::MainProcessProperty main_property_type, ProcessID pid)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcFSProcessPropertyInode(procfs, main_property_type, pid));
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(const ProcFS& procfs, SegmentedProcFSIndex::MainProcessProperty main_property_type, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(pid, main_property_type))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::Reserved)
{
    m_possible_data.property_type = main_property_type;
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(const ProcFS& procfs, unsigned file_description_index, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_file_description(pid, file_description_index))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
{
    m_possible_data.property_index = file_description_index;
}

ProcFSProcessPropertyInode::ProcFSProcessPropertyInode(const ProcFS& procfs, ThreadID thread_stack_index, ProcessID pid)
    : ProcFSProcessAssociatedInode(procfs, pid, SegmentedProcFSIndex::build_segmented_index_for_thread_stack(pid, thread_stack_index))
    , m_parent_sub_directory_type(SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
{
    m_possible_data.property_index = thread_stack_index.value();
}

ErrorOr<void> ProcFSProcessPropertyInode::attach(OpenFileDescription& description)
{
    return refresh_data(description);
}
void ProcFSProcessPropertyInode::did_seek(OpenFileDescription& description, off_t offset)
{
    if (offset != 0)
        return;
    (void)refresh_data(description);
}

static mode_t determine_procfs_process_inode_mode(SegmentedProcFSIndex::ProcessSubDirectory parent_sub_directory_type, SegmentedProcFSIndex::MainProcessProperty main_property)
{
    if (parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
        return S_IFLNK | 0400;
    if (parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
        return S_IFREG | 0400;
    VERIFY(parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::BinaryLink)
        return S_IFLNK | 0777;
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink)
        return S_IFLNK | 0777;
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::TTYLink)
        return S_IFLNK | 0777;
    return S_IFREG | 0400;
}

InodeMetadata ProcFSProcessPropertyInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return {};

    auto traits = process->procfs_traits();
    InodeMetadata metadata;
    metadata.inode = { fsid(), traits->component_index() };
    metadata.mode = determine_procfs_process_inode_mode(m_parent_sub_directory_type, m_possible_data.property_type);
    metadata.uid = traits->owner_user();
    metadata.gid = traits->owner_group();
    metadata.size = 0;
    metadata.mtime = traits->modified_time();
    return metadata;
}
ErrorOr<void> ProcFSProcessPropertyInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    VERIFY_NOT_REACHED();
}
ErrorOr<size_t> ProcFSProcessPropertyInode::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFS ProcessInformation: read_bytes offset: {} count: {}", offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description) {
        auto builder = TRY(KBufferBuilder::try_create());
        auto process = Process::from_pid(associated_pid());
        if (!process)
            return Error::from_errno(ESRCH);
        TRY(try_to_acquire_data(*process, builder));
        auto data_buffer = builder.build();
        if (!data_buffer)
            return Error::from_errno(EFAULT);
        ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
        TRY(buffer.write(data_buffer->data() + offset, nread));
        return nread;
    }
    if (!description->data()) {
        dbgln("ProcFS Process Information: Do not have cached data!");
        return Error::from_errno(EIO);
    }

    MutexLocker locker(m_refresh_lock);

    auto& typed_cached_data = static_cast<ProcFSInodeData&>(*description->data());
    auto& data_buffer = typed_cached_data.buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(data_buffer->data() + offset, nread));

    return nread;
}
ErrorOr<NonnullRefPtr<Inode>> ProcFSProcessPropertyInode::lookup(StringView)
{
    return EINVAL;
}

static ErrorOr<void> build_from_cached_data(KBufferBuilder& builder, ProcFSInodeData& cached_data)
{
    cached_data.buffer = builder.build();
    if (!cached_data.buffer)
        return ENOMEM;
    return {};
}

ErrorOr<void> ProcFSProcessPropertyInode::try_to_acquire_data(Process& process, KBufferBuilder& builder) const
{
    // FIXME: Verify process is already ref-counted
    if (m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions) {
        TRY(process.procfs_get_file_description_link(m_possible_data.property_index, builder));
        return {};
    }
    if (m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks) {
        TRY(process.procfs_get_thread_stack(m_possible_data.property_index, builder));
        return {};
    }

    VERIFY(m_parent_sub_directory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    switch (m_possible_data.property_type) {
    case SegmentedProcFSIndex::MainProcessProperty::Unveil:
        return process.procfs_get_unveil_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::Pledge:
        return process.procfs_get_pledge_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions:
        return process.procfs_get_fds_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::BinaryLink:
        return process.procfs_get_binary_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink:
        return process.procfs_get_current_work_directory_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents:
        return process.procfs_get_perf_events(builder);
    case SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats:
        return process.procfs_get_virtual_memory_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::TTYLink:
        return process.procfs_get_tty_link(builder);
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> ProcFSProcessPropertyInode::refresh_data(OpenFileDescription& description)
{
    // For process-specific inodes, hold the process's ptrace lock across refresh
    // and refuse to load data if the process is not dumpable.
    // Without this, files opened before a process went non-dumpable could still be used for dumping.
    auto process = Process::from_pid(associated_pid());
    if (!process)
        return Error::from_errno(ESRCH);
    process->ptrace_lock().lock();
    if (!process->is_dumpable()) {
        process->ptrace_lock().unlock();
        return EPERM;
    }
    ScopeGuard guard = [&] {
        process->ptrace_lock().unlock();
    };
    MutexLocker locker(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data) {
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
        if (!cached_data)
            return ENOMEM;
    }
    auto builder = TRY(KBufferBuilder::try_create());
    TRY(try_to_acquire_data(*process, builder));
    return build_from_cached_data(builder, static_cast<ProcFSInodeData&>(*cached_data));
}
}
