/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/Inode.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ProcFSInode::~ProcFSInode() = default;

static mode_t determine_procfs_process_inode_mode(u32 subdirectory, u32 property)
{
    if (subdirectory == process_fd_subdirectory_root_entry.subdirectory)
        return S_IFLNK | 0400;
    if (subdirectory == process_stacks_subdirectory_root_entry.subdirectory)
        return S_IFREG | 0400;
    if (subdirectory == process_children_subdirectory_root_entry.subdirectory)
        return S_IFLNK | 0400;
    VERIFY(subdirectory == main_process_directory_root_entry.subdirectory);
    if (property == process_exe_symlink_entry.property)
        return S_IFLNK | 0777;
    if (property == process_cwd_symlink_entry.property)
        return S_IFLNK | 0777;
    return S_IFREG | 0400;
}

static u16 extract_subdirectory_index_from_inode_index(InodeIndex inode_index)
{
    return (inode_index.value() >> 20) & 0xFFFF;
}

static u32 extract_property_index_from_inode_index(InodeIndex inode_index)
{
    return inode_index.value() & 0xFFFFF;
}

InodeIndex ProcFSInode::create_index_from_global_directory_entry(segmented_global_inode_index entry)
{
    u64 inode_index = 0;
    VERIFY(entry.primary < 0x10000000);
    u64 tmp = entry.primary;
    inode_index |= tmp << 36;

    // NOTE: The sub-directory part is already limited to 0xFFFF, so no need to VERIFY it.
    tmp = entry.subdirectory;
    inode_index |= tmp << 20;

    VERIFY(entry.property < 0x100000);
    inode_index |= entry.property;
    return inode_index;
}

InodeIndex ProcFSInode::create_index_from_process_directory_entry(ProcessID pid, segmented_process_directory_entry entry)
{
    u64 inode_index = 0;
    // NOTE: We use 0xFFFFFFF because PID part (bits 64-36) as 0 is reserved for global inodes.
    VERIFY(pid.value() < 0xFFFFFFF);
    u64 tmp = (pid.value() + 1);
    inode_index |= tmp << 36;
    // NOTE: The sub-directory part is already limited to 0xFFFF, so no need to VERIFY it.
    tmp = entry.subdirectory;
    inode_index |= tmp << 20;
    VERIFY(entry.property < 0x100000);
    inode_index |= entry.property;
    return inode_index;
}

static Optional<ProcessID> extract_possible_pid_from_inode_index(InodeIndex inode_index)
{
    auto pid_part = inode_index.value() >> 36;
    // NOTE: pid_part is set to 0 for global inodes.
    if (pid_part == 0)
        return {};
    return pid_part - 1;
}

ProcFSInode::ProcFSInode(ProcFS const& procfs_instance, InodeIndex inode_index)
    : Inode(const_cast<ProcFS&>(procfs_instance), inode_index)
    , m_associated_pid(extract_possible_pid_from_inode_index(inode_index))
    , m_subdirectory(extract_subdirectory_index_from_inode_index(inode_index))
    , m_property(extract_property_index_from_inode_index(inode_index))
{
    if (inode_index == 1) {
        m_type = Type::RootDirectory;
        return;
    }
    if (inode_index == 2) {
        m_type = Type::SelfProcessLink;
        return;
    }

    if (m_property == 0) {
        if (m_subdirectory > 0)
            m_type = Type::ProcessSubdirectory;
        else
            m_type = Type::ProcessDirectory;
        return;
    }

    m_type = Type::ProcessProperty;
}

ErrorOr<void> ProcFSInode::traverse_as_root_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    TRY(callback({ "."sv, { fsid(), to_underlying(RAMBackedFileType::Directory) }, 0 }));
    TRY(callback({ ".."sv, { fsid(), to_underlying(RAMBackedFileType::Directory) }, 0 }));
    TRY(callback({ "self"sv, { fsid(), 2 }, to_underlying(RAMBackedFileType::Link) }));

    return Process::for_each_in_same_process_list([&](Process& process) -> ErrorOr<void> {
        VERIFY(!(process.pid() < 0));
        u64 process_id = (u64)process.pid().value();
        InodeIdentifier identifier = { fsid(), static_cast<InodeIndex>(process_id << 36) };
        auto process_id_string = TRY(KString::formatted("{:d}", process_id));
        TRY(callback({ process_id_string->view(), identifier, to_underlying(RAMBackedFileType::Directory) }));
        return {};
    });
}

ErrorOr<void> ProcFSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    if (m_type == Type::ProcessSubdirectory) {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return EINVAL;
        switch (m_subdirectory) {
        case process_fd_subdirectory_root_entry.subdirectory:
            return process->traverse_file_descriptions_directory(procfs().fsid(), move(callback));
        case process_stacks_subdirectory_root_entry.subdirectory:
            return process->traverse_stacks_directory(procfs().fsid(), move(callback));
        case process_children_subdirectory_root_entry.subdirectory:
            return process->traverse_children_directory(procfs().fsid(), move(callback));
        default:
            VERIFY_NOT_REACHED();
        }
        VERIFY_NOT_REACHED();
    }

    if (m_type == Type::RootDirectory) {
        return traverse_as_root_directory(move(callback));
    }

    VERIFY(m_type == Type::ProcessDirectory);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
    if (!process)
        return EINVAL;
    return process->traverse_as_directory(procfs().fsid(), move(callback));
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSInode::lookup_as_root_directory(StringView name)
{
    if (name == "self"sv)
        return procfs().get_inode({ fsid(), 2 });

    auto pid = name.to_number<unsigned>();
    if (!pid.has_value())
        return ESRCH;
    auto actual_pid = pid.value();

    if (auto maybe_process = Process::from_pid_in_same_process_list(actual_pid)) {
        InodeIndex id = (static_cast<u64>(maybe_process->pid().value()) + 1) << 36;
        return procfs().get_inode({ fsid(), id });
    }
    return ENOENT;
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSInode::lookup(StringView name)
{
    if (m_type == Type::ProcessSubdirectory) {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return ESRCH;
        switch (m_subdirectory) {
        case process_fd_subdirectory_root_entry.subdirectory:
            return process->lookup_file_descriptions_directory(procfs(), name);
        case process_stacks_subdirectory_root_entry.subdirectory:
            return process->lookup_stacks_directory(procfs(), name);
        case process_children_subdirectory_root_entry.subdirectory:
            return process->lookup_children_directory(procfs(), name);
        default:
            VERIFY_NOT_REACHED();
        }
        VERIFY_NOT_REACHED();
    }

    if (m_type == Type::RootDirectory) {
        return lookup_as_root_directory(name);
    }

    VERIFY(m_type == Type::ProcessDirectory);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
    if (!process)
        return ESRCH;
    return process->lookup_as_directory(procfs(), name);
}

ErrorOr<void> ProcFSInode::attach(OpenFileDescription& description)
{
    if (m_type == Type::RootDirectory || m_type == Type::SelfProcessLink || m_type == Type::ProcessDirectory || m_type == Type::ProcessSubdirectory)
        return {};
    VERIFY(m_type == Type::ProcessProperty);
    return refresh_process_property_data(description);
}

void ProcFSInode::did_seek(OpenFileDescription& description, off_t offset)
{
    if (m_type == Type::SelfProcessLink) {
        return;
    }
    VERIFY(m_type == Type::ProcessProperty);
    if (offset != 0)
        return;
    (void)refresh_process_property_data(description);
}

ErrorOr<size_t> ProcFSInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSInode: read_bytes_locked offset: {} count: {}", offset, count);
    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (m_type == Type::SelfProcessLink) {
        auto builder = TRY(KBufferBuilder::try_create());
        TRY(builder.appendff("{}", Process::current().pid().value()));
        auto data_buffer = builder.build();
        if (!data_buffer)
            return Error::from_errno(EFAULT);
        if ((size_t)offset >= data_buffer->size())
            return 0;
        ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
        TRY(buffer.write(data_buffer->data() + offset, nread));
        return nread;
    }

    VERIFY(m_type == Type::ProcessProperty);

    if (!description) {
        auto builder = TRY(KBufferBuilder::try_create());
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return Error::from_errno(ESRCH);
        TRY(try_fetch_process_property_data(*process, builder));
        auto data_buffer = builder.build();
        if (!data_buffer)
            return Error::from_errno(EFAULT);
        if ((size_t)offset >= data_buffer->size())
            return 0;
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

ErrorOr<void> ProcFSInode::try_fetch_process_property_data(NonnullRefPtr<Process> process, KBufferBuilder& builder) const
{
    VERIFY(m_type == Type::ProcessProperty);
    if (m_subdirectory == process_fd_subdirectory_root_entry.subdirectory) {
        // NOTE: All property numbers should start from 1 as 0 is reserved for the directory itself.
        // Therefore subtract 1 to get the actual correct fd number.
        TRY(process->procfs_get_file_description_link(m_property - 1, builder));
        return {};
    }
    if (m_subdirectory == process_stacks_subdirectory_root_entry.subdirectory) {
        // NOTE: All property numbers should start from 1 as 0 is reserved for the directory itself.
        // Therefore subtract 1 to get the actual correct thread stack number.
        TRY(process->procfs_get_thread_stack(m_property - 1, builder));
        return {};
    }
    if (m_subdirectory == process_children_subdirectory_root_entry.subdirectory) {
        // NOTE: All property numbers should start from 1 as 0 is reserved for the directory itself.
        // Therefore subtract 1 to get the actual correct child process index number for a correct symlink.
        TRY(process->procfs_get_child_process_link(m_property - 1, builder));
        return {};
    }

    VERIFY(m_subdirectory == main_process_directory_root_entry.subdirectory);
    switch (m_property) {
    case process_unveil_list_entry.property:
        return process->procfs_get_unveil_stats(builder);
    case process_pledge_list_entry.property:
        return process->procfs_get_pledge_stats(builder);
    case process_fds_list_entry.property:
        return process->procfs_get_fds_stats(builder);
    case process_exe_symlink_entry.property:
        return process->procfs_get_binary_link(builder);
    case process_cwd_symlink_entry.property:
        return process->procfs_get_current_work_directory_link(builder);
    case process_perf_events_entry.property:
        return process->procfs_get_perf_events(builder);
    case process_vm_entry.property:
        return process->procfs_get_virtual_memory_stats(builder);
    case process_cmdline_entry.property:
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
    VERIFY(m_type == Type::ProcessProperty);
    VERIFY(m_associated_pid.has_value());
    auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
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
    case Type::SelfProcessLink: {
        metadata.inode = { fsid(), 2 };
        metadata.mode = S_IFLNK | 0777;
        metadata.uid = 0;
        metadata.gid = 0;
        metadata.size = 0;
        metadata.mtime = TimeManagement::boot_time();
        break;
    }
    case Type::RootDirectory: {
        metadata.inode = { fsid(), 1 };
        metadata.mode = S_IFDIR | 0555;
        metadata.uid = 0;
        metadata.gid = 0;
        metadata.size = 0;
        metadata.mtime = TimeManagement::boot_time();
        break;
    }
    case Type::ProcessProperty: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return {};
        metadata.inode = identifier();
        metadata.mode = determine_procfs_process_inode_mode(m_subdirectory, m_property);
        auto credentials = process->credentials();
        metadata.uid = credentials->uid();
        metadata.gid = credentials->gid();
        metadata.size = 0;
        auto creation_time = process->creation_time();
        metadata.atime = creation_time;
        metadata.ctime = creation_time;
        metadata.mtime = creation_time;
        break;
    }
    case Type::ProcessDirectory: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return {};
        metadata.inode = identifier();
        metadata.mode = S_IFDIR | 0555;
        auto credentials = process->credentials();
        metadata.uid = credentials->uid();
        metadata.gid = credentials->gid();
        metadata.size = 0;
        auto creation_time = process->creation_time();
        metadata.atime = creation_time;
        metadata.ctime = creation_time;
        metadata.mtime = creation_time;
        break;
    }
    case Type::ProcessSubdirectory: {
        VERIFY(m_associated_pid.has_value());
        auto process = Process::from_pid_in_same_process_list(m_associated_pid.value());
        if (!process)
            return {};
        metadata.inode = identifier();
        metadata.mode = S_IFDIR | 0555;
        auto credentials = process->credentials();
        metadata.uid = credentials->uid();
        metadata.gid = credentials->gid();
        metadata.size = 0;
        auto creation_time = process->creation_time();
        metadata.atime = creation_time;
        metadata.ctime = creation_time;
        metadata.mtime = creation_time;
        break;
    }
    }
    return metadata;
}

}
