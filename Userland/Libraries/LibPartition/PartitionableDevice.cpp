/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPartition/PartitionableDevice.h>

#ifndef KERNEL
#    include <sys/ioctl.h>
#endif

namespace Partition {

#ifdef KERNEL
ErrorOr<PartitionableDevice> PartitionableDevice::create(Kernel::StorageDevice& device)
{
    return PartitionableDevice(device);
}
#else
ErrorOr<PartitionableDevice> PartitionableDevice::create(MaybeOwned<Core::File> device_file)
{
    VERIFY(device_file.ptr() != nullptr);
    size_t block_size;
    int rc = ioctl(device_file->fd(), STORAGE_DEVICE_GET_BLOCK_SIZE, &block_size);
    if (rc < 0)
        return Error::from_string_view("ioctl on device failed"sv);
    return PartitionableDevice(move(device_file), block_size);
}
#endif

#ifdef KERNEL
PartitionableDevice::PartitionableDevice(Kernel::StorageDevice& device)
    : m_device(device)
{
}
#else
PartitionableDevice::PartitionableDevice(MaybeOwned<Core::File> device_file, size_t block_size)
    : m_device_file(move(device_file))
    , m_block_size(block_size)
{
}
#endif

#ifdef KERNEL
PartitionableDevice PartitionableDevice::clone_unowned()
{
    return PartitionableDevice(m_device);
}
#else
PartitionableDevice PartitionableDevice::clone_unowned()
{
    return PartitionableDevice(MaybeOwned<Core::File>(*m_device_file), m_block_size);
}
#endif

#ifdef KERNEL
ErrorOr<PartitionableDevice> PartitionableDevice::clone_owned()
{
    return PartitionableDevice(m_device);
}
#else
ErrorOr<PartitionableDevice> PartitionableDevice::clone_owned()
{
    auto cloned_file = TRY(Core::File::adopt_fd(m_device_file->fd(), Core::File::OpenMode::Read, Core::File::ShouldCloseFileDescriptor::No));
    return PartitionableDevice(move(cloned_file), m_block_size);
}
#endif

#ifdef KERNEL
size_t PartitionableDevice::block_size() const
{
    return m_device.block_size();
}
#else
size_t PartitionableDevice::block_size() const
{
    return m_block_size;
}
#endif

#ifdef KERNEL
ErrorOr<void> PartitionableDevice::read_block(size_t block_index, Bytes block_buffer)
{
    VERIFY(block_buffer.size() == block_size());
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block_buffer.data());
    bool read_successful = m_device.read_block(block_index, buffer);
    if (!read_successful)
        return Error::from_errno(EIO);
    return {};
}
#else
ErrorOr<void> PartitionableDevice::read_block(size_t block_index, Bytes block_buffer)
{
    VERIFY(block_buffer.size() == block_size());
    TRY(m_device_file->seek(block_index * block_size(), SeekMode::SetPosition));
    TRY(m_device_file->read_until_filled(block_buffer));
    return {};
}
#endif

}
