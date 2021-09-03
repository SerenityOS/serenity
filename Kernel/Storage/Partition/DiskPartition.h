/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/RefPtr.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Storage/Partition/DiskPartitionMetadata.h>

namespace Kernel {

class DiskPartition final : public BlockDevice {
public:
    static NonnullRefPtr<DiskPartition> create(BlockDevice&, unsigned, DiskPartitionMetadata);
    virtual ~DiskPartition();

    virtual void start_request(AsyncBlockDeviceRequest&) override;

    // ^BlockDevice
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;

    // ^Device
    virtual mode_t required_mode() const override { return 0600; }
    virtual String device_name() const override;

    const DiskPartitionMetadata& metadata() const;

private:
    virtual StringView class_name() const override;

    DiskPartition(BlockDevice&, unsigned, DiskPartitionMetadata);

    NonnullRefPtr<BlockDevice> m_device;
    DiskPartitionMetadata m_metadata;
};

}
