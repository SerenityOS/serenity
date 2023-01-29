/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/Inode.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

ProcFSInode::~ProcFSInode() = default;

ErrorOr<void> ProcFSInode::flush_metadata()
{
    return {};
}

ErrorOr<void> ProcFSInode::add_child(Inode&, StringView, mode_t)
{
    return EROFS;
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
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

ErrorOr<void> ProcFSInode::replace_child(StringView, Inode&)
{
    return EROFS;
}

ErrorOr<size_t> ProcFSInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::truncate(u64)
{
    return EROFS;
}

ErrorOr<void> ProcFSInode::update_timestamps(Optional<Time>, Optional<Time>, Optional<Time>)
{
    // Note: Silently ignore the update request.
    return {};
}

static mode_t determine_procfs_process_inode_mode(SegmentedProcFSIndex::ProcessSubDirectory parent_subdirectory_type, Optional<SegmentedProcFSIndex::MainProcessProperty> main_property)
{
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
        return S_IFLNK | 0400;
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
        return S_IFREG | 0400;
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Children)
        return S_IFLNK | 0400;
    VERIFY(parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::BinaryLink)
        return S_IFLNK | 0777;
    if (main_property == SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink)
        return S_IFLNK | 0777;
    return S_IFREG | 0400;
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_file_description_link_inode(ProcFS const& procfs_instance, unsigned fd_number, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, fd_number, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, unsigned file_description_index, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_file_description(pid, file_description_index))
    , m_type(Type::FileDescriptionLink)
    , m_parent_subdirectory_type(SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions)
    , m_associated_pid(pid)
{
    m_possible_data.property_index = file_description_index;
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_thread_stack_inode(ProcFS const& procfs_instance, ThreadID stack_thread_index, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, stack_thread_index, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, ThreadID thread_stack_index, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_thread_stack(pid, thread_stack_index))
    , m_type(Type::ThreadStack)
    , m_parent_subdirectory_type(SegmentedProcFSIndex::ProcessSubDirectory::Stacks)
    , m_associated_pid(pid)
{
    m_possible_data.property_index = thread_stack_index.value();
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_pid_property_inode(ProcFS const& procfs_instance, SegmentedProcFSIndex::MainProcessProperty process_property, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, process_property, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, SegmentedProcFSIndex::MainProcessProperty main_property_type, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(pid, main_property_type))
    , m_type(Type::ProcessProperty)
    , m_parent_subdirectory_type(SegmentedProcFSIndex::ProcessSubDirectory::Reserved)
    , m_associated_pid(pid)
{
    m_possible_data.property_type = main_property_type;
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_child_process_link_inode(ProcFS const& procfs_instance, ProcessID child_pid, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, child_pid, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, ProcessID child_pid, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_children(pid, child_pid))
    , m_type(Type::ChildProcessLink)
    , m_parent_subdirectory_type(SegmentedProcFSIndex::ProcessSubDirectory::Children)
    , m_associated_pid(pid)
{
    m_possible_data.property_index = child_pid.value();
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_process_directory_inode(ProcFS const& procfs_instance, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_pid_directory(pid))
    , m_type(Type::ProcessDirectory)
    , m_associated_pid(pid)
{
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_process_subdirectory_inode(ProcFS const& procfs_instance, SegmentedProcFSIndex::ProcessSubDirectory subdirectory_type, ProcessID pid)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, subdirectory_type, pid));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, SegmentedProcFSIndex::ProcessSubDirectory subdirectory_type, ProcessID pid)
    : Inode(const_cast<ProcFS&>(procfs_instance), SegmentedProcFSIndex::build_segmented_index_for_sub_directory(pid, subdirectory_type))
    , m_type(Type::ProcessSubdirectory)
    , m_subdirectory_type(subdirectory_type)
    , m_associated_pid(pid)
{
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_global_link_inode(ProcFS const& procfs_instance, ProcFSExposedLink const& link_component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, link_component));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, ProcFSExposedLink const& link_component)
    : Inode(const_cast<ProcFS&>(procfs_instance), link_component.component_index())
    , m_type(Type::GlobalLink)
    , m_associated_component(link_component)
{
}

ErrorOr<NonnullLockRefPtr<ProcFSInode>> ProcFSInode::try_create_as_directory_inode(ProcFS const& procfs_instance, ProcFSExposedDirectory const& directory_component)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ProcFSInode(procfs_instance, directory_component));
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, ProcFSExposedDirectory const& directory_component)
    : Inode(const_cast<ProcFS&>(procfs_instance), directory_component.component_index())
    , m_type(Type::GlobalDirectory)
    , m_associated_component(directory_component)
{
}

ErrorOr<void> ProcFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(procfs().m_lock);
    if (m_type == Type::ProcessSubdirectory) {
        VERIFY(m_associated_pid.has_value());
        VERIFY(m_subdirectory_type.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return EINVAL;
        switch (m_subdirectory_type.value()) {
        case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
            return process->traverse_file_descriptions_directory(procfs().fsid(), move(callback));
        case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
            return process->traverse_stacks_directory(procfs().fsid(), move(callback));
        case SegmentedProcFSIndex::ProcessSubDirectory::Children:
            return process->traverse_children_directory(procfs().fsid(), move(callback));
        default:
            VERIFY_NOT_REACHED();
        }
        VERIFY_NOT_REACHED();
    }

    if (m_type == Type::GlobalDirectory) {
        VERIFY(m_associated_component);
        return m_associated_component->traverse_as_directory(procfs().fsid(), move(callback));
    }
    VERIFY(m_type == Type::ProcessDirectory);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
    if (!process)
        return EINVAL;
    return process->procfs_traits()->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSInode::lookup(StringView name)
{
    MutexLocker locker(procfs().m_lock);
    if (m_type == Type::ProcessSubdirectory) {
        VERIFY(m_associated_pid.has_value());
        VERIFY(m_subdirectory_type.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return ESRCH;
        switch (m_subdirectory_type.value()) {
        case SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions:
            return process->lookup_file_descriptions_directory(procfs(), name);
        case SegmentedProcFSIndex::ProcessSubDirectory::Stacks:
            return process->lookup_stacks_directory(procfs(), name);
        case SegmentedProcFSIndex::ProcessSubDirectory::Children:
            return process->lookup_children_directory(procfs(), name);
        default:
            VERIFY_NOT_REACHED();
        }
        VERIFY_NOT_REACHED();
    }

    if (m_type == Type::GlobalDirectory) {
        VERIFY(m_associated_component);
        auto component = TRY(m_associated_component->lookup(name));
        return TRY(component->to_inode(procfs()));
    }

    VERIFY(m_type == Type::ProcessDirectory);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
    if (!process)
        return ESRCH;
    if (name == "fd"sv)
        return TRY(ProcFSInode::try_create_as_process_subdirectory_inode(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions, m_associated_pid.value()));
    if (name == "stacks"sv)
        return TRY(ProcFSInode::try_create_as_process_subdirectory_inode(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks, m_associated_pid.value()));
    if (name == "children"sv)
        return TRY(ProcFSInode::try_create_as_process_subdirectory_inode(procfs(), SegmentedProcFSIndex::ProcessSubDirectory::Children, m_associated_pid.value()));
    if (name == "unveil"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::Unveil, m_associated_pid.value()));
    if (name == "pledge"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::Pledge, m_associated_pid.value()));
    if (name == "fds"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions, m_associated_pid.value()));
    if (name == "exe"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink, m_associated_pid.value()));
    if (name == "cwd"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink, m_associated_pid.value()));
    if (name == "perf_events"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents, m_associated_pid.value()));
    if (name == "vm"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats, m_associated_pid.value()));
    if (name == "cmdline"sv)
        return TRY(ProcFSInode::try_create_as_pid_property_inode(procfs(), SegmentedProcFSIndex::MainProcessProperty::CommandLine, m_associated_pid.value()));
    return ENOENT;
}

ErrorOr<void> ProcFSInode::attach(OpenFileDescription& description)
{
    if (m_type == Type::GlobalDirectory || m_type == Type::ProcessDirectory || m_type == Type::ProcessSubdirectory)
        return {};
    if (m_type == Type::GlobalLink)
        return m_associated_component->refresh_data(description);
    VERIFY(m_type == Type::ProcessProperty || m_type == Type::FileDescriptionLink || m_type == Type::ThreadStack || m_type == Type::ChildProcessLink);
    return refresh_process_property_data(description);
}

void ProcFSInode::did_seek(OpenFileDescription& description, off_t offset)
{
    if (m_type == Type::GlobalLink) {
        if (offset != 0)
            return;
        auto result = m_associated_component->refresh_data(description);
        if (result.is_error()) {
            // Subsequent calls to read will return EIO!
            dbgln("ProcFS: Could not refresh contents: {}", result.error());
        }
    }
    VERIFY(m_type == Type::ProcessProperty || m_type == Type::FileDescriptionLink || m_type == Type::ThreadStack || m_type == Type::ChildProcessLink);
    if (offset != 0)
        return;
    (void)refresh_process_property_data(description);
}

ErrorOr<size_t> ProcFSInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSInode: read_bytes_locked offset: {} count: {}", offset, count);
    if (m_type == Type::GlobalLink) {
        VERIFY(m_associated_component);
        return m_associated_component->read_bytes(offset, count, buffer, description);
    }

    VERIFY(m_type == Type::ProcessProperty || m_type == Type::FileDescriptionLink || m_type == Type::ThreadStack || m_type == Type::ChildProcessLink);
    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description) {
        auto builder = TRY(KBufferBuilder::try_create());
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return Error::from_errno(ESRCH);
        TRY(try_fetch_process_property_data(*process, builder));
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

static ErrorOr<void> build_from_cached_data(KBufferBuilder& builder, ProcFSInodeData& cached_data)
{
    cached_data.buffer = builder.build();
    if (!cached_data.buffer)
        return ENOMEM;
    return {};
}

ErrorOr<void> ProcFSInode::try_fetch_process_property_data(NonnullLockRefPtr<Process> process, KBufferBuilder& builder) const
{
    VERIFY(m_type == Type::ProcessProperty || m_type == Type::FileDescriptionLink || m_type == Type::ThreadStack || m_type == Type::ChildProcessLink);
    VERIFY(m_parent_subdirectory_type.has_value());
    auto parent_subdirectory_type = m_parent_subdirectory_type.value();
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions) {
        TRY(process->procfs_get_file_description_link(m_possible_data.property_index, builder));
        return {};
    }
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Stacks) {
        TRY(process->procfs_get_thread_stack(m_possible_data.property_index, builder));
        return {};
    }
    if (parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Children) {
        TRY(process->procfs_get_child_proccess_link(m_possible_data.property_index, builder));
        return {};
    }

    VERIFY(m_type == Type::ProcessProperty);
    VERIFY(parent_subdirectory_type == SegmentedProcFSIndex::ProcessSubDirectory::Reserved);
    switch (m_possible_data.property_type) {
    case SegmentedProcFSIndex::MainProcessProperty::Unveil:
        return process->procfs_get_unveil_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::Pledge:
        return process->procfs_get_pledge_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions:
        return process->procfs_get_fds_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::BinaryLink:
        return process->procfs_get_binary_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink:
        return process->procfs_get_current_work_directory_link(builder);
    case SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents:
        return process->procfs_get_perf_events(builder);
    case SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats:
        return process->procfs_get_virtual_memory_stats(builder);
    case SegmentedProcFSIndex::MainProcessProperty::CommandLine:
        return process->procfs_get_command_line(builder);
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> ProcFSInode::refresh_process_property_data(OpenFileDescription& description)
{
    // For process-specific inodes, hold the process's ptrace lock across refresh
    // and refuse to load data if the process is not dumpable.
    // Without this, files opened before a process went non-dumpable could still be used for dumping.
    VERIFY(m_type == Type::ProcessProperty || m_type == Type::FileDescriptionLink || m_type == Type::ThreadStack || m_type == Type::ChildProcessLink);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
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
    TRY(try_fetch_process_property_data(*process, builder));
    return build_from_cached_data(builder, static_cast<ProcFSInodeData&>(*cached_data));
}

InodeMetadata ProcFSInode::metadata() const
{
    InodeMetadata metadata;
    switch (m_type) {
    case Type::GlobalLink: {
        metadata.inode = { fsid(), m_associated_component->component_index() };
        metadata.mode = S_IFLNK | m_associated_component->required_mode();
        metadata.uid = m_associated_component->owner_user();
        metadata.gid = m_associated_component->owner_group();
        metadata.size = 0;
        metadata.mtime = m_associated_component->modified_time();
        break;
    }
    case Type::GlobalDirectory: {
        metadata.inode = { fsid(), m_associated_component->component_index() };
        metadata.mode = S_IFDIR | m_associated_component->required_mode();
        metadata.uid = m_associated_component->owner_user();
        metadata.gid = m_associated_component->owner_group();
        metadata.size = 0;
        metadata.mtime = m_associated_component->modified_time();
        break;
    }
    case Type::FileDescriptionLink: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), SegmentedProcFSIndex::build_segmented_index_for_file_description(m_associated_pid.value(), m_possible_data.property_index) };
        metadata.mode = determine_procfs_process_inode_mode(m_parent_subdirectory_type.value(), {});
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    case Type::ThreadStack: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), SegmentedProcFSIndex::build_segmented_index_for_thread_stack(m_associated_pid.value(), m_possible_data.property_index) };
        metadata.mode = determine_procfs_process_inode_mode(m_parent_subdirectory_type.value(), {});
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    case Type::ProcessProperty: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(m_associated_pid.value(), m_possible_data.property_type) };
        metadata.mode = determine_procfs_process_inode_mode(m_parent_subdirectory_type.value(), m_possible_data.property_type);
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    case Type::ChildProcessLink: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), SegmentedProcFSIndex::build_segmented_index_for_children(m_associated_pid.value(), m_possible_data.property_index) };
        metadata.mode = determine_procfs_process_inode_mode(m_parent_subdirectory_type.value(), {});
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    case Type::ProcessDirectory: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), traits->component_index() };
        metadata.mode = S_IFDIR | traits->required_mode();
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    case Type::ProcessSubdirectory: {
        VERIFY(m_associated_pid.has_value());
        VERIFY(m_subdirectory_type.has_value());
        auto process = Process::from_pid_in_same_jail(m_associated_pid.value());
        if (!process)
            return {};
        auto traits = process->procfs_traits();
        metadata.inode = { fsid(), SegmentedProcFSIndex::build_segmented_index_for_sub_directory(m_associated_pid.value(), m_subdirectory_type.value()) };
        metadata.mode = S_IFDIR | traits->required_mode();
        metadata.uid = traits->owner_user();
        metadata.gid = traits->owner_group();
        metadata.size = 0;
        metadata.mtime = traits->modified_time();
        break;
    }
    }
    return metadata;
}

}
