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

static SpinLock<u8> s_index_lock;
static InodeIndex s_next_inode_index = 0;

static size_t s_allocate_inode_index()
{
    ScopedSpinLock lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    VERIFY(s_next_inode_index > 0);
    return s_next_inode_index.value();
}

InodeIndex ProcFSComponentRegistry::allocate_inode_index() const
{
    return s_allocate_inode_index();
}

ProcFSExposedComponent::ProcFSExposedComponent(StringView name)
    : m_component_index(s_allocate_inode_index())
{
    m_name = KString::try_create(name);
}

// Note: This constructor is intended to be used in /proc/pid/fd/* symlinks
// so we preallocated inode index for them so we just need to set it here.
ProcFSExposedComponent::ProcFSExposedComponent(StringView name, InodeIndex preallocated_index)
    : m_component_index(preallocated_index.value())
{
    VERIFY(preallocated_index.value() != 0);
    VERIFY(preallocated_index <= s_next_inode_index);
    m_name = KString::try_create(name);
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

ProcFSExposedLink::ProcFSExposedLink(StringView name, InodeIndex preallocated_index)
    : ProcFSExposedComponent(name, preallocated_index)
{
}

struct ProcFSInodeData : public FileDescriptionData {
    RefPtr<KBufferImpl> buffer;
};

KResultOr<size_t> ProcFSGlobalInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSGlobalInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return KResult(EIO);
    if (!description->data()) {
        dbgln("ProcFSGlobalInformation: Do not have cached data!");
        return KResult(EIO);
    }

    // Be sure to keep a reference to data_buffer while we use it!
    RefPtr<KBufferImpl> data_buffer = static_cast<ProcFSInodeData&>(*description->data()).buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data_buffer->data() + offset, nread))
        return KResult(EFAULT);

    return nread;
}

KResult ProcFSGlobalInformation::refresh_data(FileDescription& description) const
{
    ScopedSpinLock lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data)
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
    VERIFY(description.data());
    auto& buffer = static_cast<ProcFSInodeData&>(*cached_data).buffer;
    if (buffer) {
        // If we're reusing the buffer, reset the size to 0 first. This
        // ensures we don't accidentally leak previously written data.
        buffer->set_size(0);
    }
    KBufferBuilder builder(buffer, true);
    if (!const_cast<ProcFSGlobalInformation&>(*this).output(builder))
        return ENOENT;
    // We don't use builder.build() here, which would steal our buffer
    // and turn it into an OwnPtr. Instead, just flush to the buffer so
    // that we can read all the data that was written.
    if (!builder.flush())
        return ENOMEM;
    if (!buffer)
        return ENOMEM;
    return KSuccess;
}

KResultOr<size_t> ProcFSProcessInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    dbgln_if(PROCFS_DEBUG, "ProcFSProcessInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return KResult(EIO);
    if (!description->data()) {
        dbgln("ProcFSGlobalInformation: Do not have cached data!");
        return KResult(EIO);
    }

    // Be sure to keep a reference to data_buffer while we use it!
    RefPtr<KBufferImpl> data_buffer = static_cast<ProcFSInodeData&>(*description->data()).buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(data_buffer->data() + offset, nread))
        return KResult(EFAULT);

    return nread;
}

KResult ProcFSProcessInformation::refresh_data(FileDescription& description) const
{
    // For process-specific inodes, hold the process's ptrace lock across refresh
    // and refuse to load data if the process is not dumpable.
    // Without this, files opened before a process went non-dumpable could still be used for dumping.
    auto parent_directory = const_cast<ProcFSProcessInformation&>(*this).m_parent_directory.strong_ref();
    if (parent_directory.is_null())
        return KResult(EINVAL);
    auto process = parent_directory->associated_process();
    if (!process)
        return KResult(ESRCH);
    process->ptrace_lock().lock();
    if (!process->is_dumpable()) {
        process->ptrace_lock().unlock();
        return EPERM;
    }
    ScopeGuard guard = [&] {
        process->ptrace_lock().unlock();
    };
    ScopedSpinLock lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data)
        cached_data = adopt_own_if_nonnull(new (nothrow) ProcFSInodeData);
    VERIFY(description.data());
    auto& buffer = static_cast<ProcFSInodeData&>(*cached_data).buffer;
    if (buffer) {
        // If we're reusing the buffer, reset the size to 0 first. This
        // ensures we don't accidentally leak previously written data.
        buffer->set_size(0);
    }
    KBufferBuilder builder(buffer, true);
    if (!const_cast<ProcFSProcessInformation&>(*this).output(builder))
        return ENOENT;
    // We don't use builder.build() here, which would steal our buffer
    // and turn it into an OwnPtr. Instead, just flush to the buffer so
    // that we can read all the data that was written.
    if (!builder.flush())
        return ENOMEM;
    if (!buffer)
        return ENOMEM;
    return KSuccess;
}

KResultOr<size_t> ProcFSExposedLink::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    VERIFY(offset == 0);
    MutexLocker locker(m_lock);
    KBufferBuilder builder;
    if (!const_cast<ProcFSExposedLink&>(*this).acquire_link(builder))
        return KResult(EFAULT);
    auto blob = builder.build();
    if (!blob)
        return KResult(EFAULT);

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(blob->data() + offset, nread))
        return KResult(EFAULT);
    return nread;
}

NonnullRefPtr<Inode> ProcFSExposedLink::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSLinkInode::create(procfs_instance, *this);
}

NonnullRefPtr<Inode> ProcFSExposedComponent::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSInode::create(procfs_instance, *this);
}

NonnullRefPtr<Inode> ProcFSExposedDirectory::to_inode(const ProcFS& procfs_instance) const
{
    return ProcFSDirectoryInode::create(procfs_instance, *this);
}

void ProcFSExposedDirectory::add_component(const ProcFSExposedComponent&)
{
    TODO();
}

RefPtr<ProcFSExposedComponent> ProcFSExposedDirectory::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return {};
}

KResult ProcFSExposedDirectory::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    auto parent_directory = m_parent_directory.strong_ref();
    if (parent_directory.is_null())
        return KResult(EINVAL);
    callback({ ".", { fsid, component_index() }, 0 });
    callback({ "..", { fsid, parent_directory->component_index() }, 0 });

    for (auto& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        callback({ component.name(), identifier, 0 });
    }
    return KSuccess;
}

}
