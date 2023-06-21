/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>

#ifdef KERNEL
#    include <Kernel/Devices/Storage/StorageDevice.h>
#else
#    include <AK/MaybeOwned.h>
#    include <LibCore/File.h>
#endif

namespace Partition {

class PartitionableDevice {
    AK_MAKE_NONCOPYABLE(PartitionableDevice);

public:
#ifdef KERNEL
    PartitionableDevice(Kernel::StorageDevice&);
    // Userland doesn't get an implicit constructor.
#endif
    PartitionableDevice(PartitionableDevice&&) = default;
    // Unused, and "move out of reference" isn't well-defined anyway:
    PartitionableDevice& operator=(PartitionableDevice&&) = delete;

#ifdef KERNEL
    static ErrorOr<PartitionableDevice> create(Kernel::StorageDevice& device);
#else
    static ErrorOr<PartitionableDevice> create(MaybeOwned<Core::File> device_file);
#endif
    ~PartitionableDevice() = default;

    PartitionableDevice clone_unowned();
    ErrorOr<PartitionableDevice> clone_owned();
    size_t block_size() const;
    ErrorOr<void> read_block(size_t block_index, Bytes block_buffer);

private:
#ifdef KERNEL
    Kernel::StorageDevice& m_device;
#else
    explicit PartitionableDevice(MaybeOwned<Core::File>, size_t block_size);
    MaybeOwned<Core::File> m_device_file;
    size_t m_block_size;
#endif
};

}
