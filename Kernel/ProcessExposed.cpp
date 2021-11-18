/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

static Spinlock s_index_lock;
static InodeIndex s_next_inode_index = 0;

namespace SegmentedProcFSIndex {
static InodeIndex __build_raw_segmented_index(u32 primary, u16 sub_directory, u32 property)
{
    VERIFY(primary < 0x10000000);
    VERIFY(property < 0x100000);
    // Note: The sub-directory part is already limited to 0xFFFF, so no need to VERIFY it.
    return static_cast<u64>((static_cast<u64>(primary) << 36) | (static_cast<u64>(sub_directory) << 20) | property);
}

static InodeIndex build_segmented_index_with_known_pid(ProcessID pid, u16 sub_directory, u32 property)
{
    return __build_raw_segmented_index(pid.value() + 1, sub_directory, property);
}

static InodeIndex build_segmented_index_with_unknown_property(ProcessID pid, ProcessSubDirectory sub_directory, unsigned property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(sub_directory), static_cast<u32>(property));
}

InodeIndex build_segmented_index_for_pid_directory(ProcessID pid)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::Reserved, to_underlying(MainProcessProperty::Reserved));
}

InodeIndex build_segmented_index_for_sub_directory(ProcessID pid, ProcessSubDirectory sub_directory)
{
    return build_segmented_index_with_unknown_property(pid, sub_directory, to_underlying(MainProcessProperty::Reserved));
}

InodeIndex build_segmented_index_for_main_property(ProcessID pid, ProcessSubDirectory sub_directory, MainProcessProperty property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(sub_directory), to_underlying(property));
}

InodeIndex build_segmented_index_for_main_property_in_pid_directory(ProcessID pid, MainProcessProperty property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(ProcessSubDirectory::Reserved), to_underlying(property));
}

InodeIndex build_segmented_index_for_thread_stack(ProcessID pid, ThreadID thread_id)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::Stacks, thread_id.value());
}

InodeIndex build_segmented_index_for_file_description(ProcessID pid, unsigned fd)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::OpenFileDescriptions, fd);
}

}

static size_t s_allocate_global_inode_index()
{
    SpinlockLocker lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    // Note: Global ProcFS indices must be above 0 and up to maximum of what 36 bit (2 ^ 36 - 1) can represent.
    VERIFY(s_next_inode_index > 0);
    VERIFY(s_next_inode_index < 0x100000000);
    return s_next_inode_index.value();
}

ProcFSExposedComponent::ProcFSExposedComponent()
{
}

ProcFSExposedComponent::ProcFSExposedComponent(StringView name)
    : m_component_index(s_allocate_global_inode_index())
{
    auto name_or_error = KString::try_create(name);
    if (name_or_error.is_error())
        TODO();
    m_name = name_or_error.release_value();
}

ProcFSExposedDirectory::ProcFSExposedDirectory(StringView name)
    : ProcFSExposedComponent(name)
{
}

ProcFSExposedDirectory::ProcFSExposedDirectory(StringView name, const ProcFSExposedDirectory& parent_directory)
    : ProcFSExposedComponent(name)
    , m_parent_directory(parent_directory)
{
}

ProcFSExposedLink::ProcFSExposedLink(StringView name)
    : ProcFSExposedComponent(name)
{
}
ErrorOr<size_t> ProcFSGlobalInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSGlobalInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return Error::from_errno(EIO);

    MutexLocker locker(m_refresh_lock);

    if (!description->data()) {
        dbgln("ProcFSGlobalInformation: Do not have cached data!");
        return Error::from_errno(EIO);
    }

    auto& typed_cached_data = static_cast<ProcFSInodeData&>(*description->data());
    auto& data_buffer = typed_cached_data.buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(data_buffer->data() + offset, nread));
    return nread;
}

ErrorOr<void> ProcFSGlobalInformation::refresh_data(OpenFileDescription& description) const
{
    MutexLocker lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data) {
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
        if (!cached_data)
            return ENOMEM;
    }
    auto builder = TRY(KBufferBuilder::try_create());
    TRY(const_cast<ProcFSGlobalInformation&>(*this).try_generate(builder));
    auto& typed_cached_data = static_cast<ProcFSInodeData&>(*cached_data);
    typed_cached_data.buffer = builder.build();
    if (!typed_cached_data.buffer)
        return ENOMEM;
    return {};
}

ErrorOr<void> ProcFSSystemBoolean::try_generate(KBufferBuilder& builder)
{
    return builder.appendff("{}\n", static_cast<int>(value()));
}

ErrorOr<size_t> ProcFSSystemBoolean::write_bytes(off_t, size_t count, const UserOrKernelBuffer& buffer, OpenFileDescription*)
{
    if (count != 1)
        return EINVAL;
    MutexLocker locker(m_refresh_lock);
    char value = 0;
    TRY(buffer.read(&value, 1));
    if (value == '0')
        set_value(false);
    else if (value == '1')
        set_value(true);
    else
        return EINVAL;
    return 1;
}

ErrorOr<void> ProcFSSystemBoolean::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

ErrorOr<void> ProcFSSystemBoolean::set_mtime(time_t)
{
    return {};
}

ErrorOr<size_t> ProcFSExposedLink::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    VERIFY(offset == 0);
    MutexLocker locker(m_lock);
    auto builder = TRY(KBufferBuilder::try_create());
    if (!const_cast<ProcFSExposedLink&>(*this).acquire_link(builder))
        return Error::from_errno(EFAULT);
    auto blob = builder.build();
    if (!blob)
        return Error::from_errno(EFAULT);

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSExposedLink::to_inode(const ProcFS& procfs_instance) const
{
    return TRY(ProcFSLinkInode::try_create(procfs_instance, *this));
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSExposedComponent::to_inode(const ProcFS& procfs_instance) const
{
    return TRY(ProcFSGlobalInode::try_create(procfs_instance, *this));
}

ErrorOr<NonnullRefPtr<Inode>> ProcFSExposedDirectory::to_inode(const ProcFS& procfs_instance) const
{
    return TRY(ProcFSDirectoryInode::try_create(procfs_instance, *this));
}

void ProcFSExposedDirectory::add_component(const ProcFSExposedComponent&)
{
    TODO();
}

ErrorOr<NonnullRefPtr<ProcFSExposedComponent>> ProcFSExposedDirectory::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return ENOENT;
}

ErrorOr<void> ProcFSExposedDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    auto parent_directory = m_parent_directory.strong_ref();
    if (parent_directory.is_null())
        return Error::from_errno(EINVAL);
    TRY(callback({ ".", { fsid, component_index() }, DT_DIR }));
    TRY(callback({ "..", { fsid, parent_directory->component_index() }, DT_DIR }));

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }
    return {};
}

}
