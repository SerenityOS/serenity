/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <LibPartition/DiskPartitionMetadata.h>

namespace Kernel {

class StorageDevice;
class StorageDevicePartition final : public BlockDevice {
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<StorageDevicePartition>> create(StorageDevice&, MinorNumber, Partition::DiskPartitionMetadata);
    virtual ~StorageDevicePartition();

    virtual void start_request(AsyncBlockDeviceRequest&) override;

    // ^BlockDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;

    // NOTE: Technically we need to query the underlying StorageDevice,
    // but since it will always return true in both of these methods,
    // we can return true here too.
    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    Partition::DiskPartitionMetadata const& metadata() const;

private:
    StorageDevicePartition(StorageDevice&, MinorNumber, Partition::DiskPartitionMetadata);
    virtual StringView class_name() const override;

    LockWeakPtr<StorageDevice> m_device;
    Partition::DiskPartitionMetadata m_metadata;
};

}
