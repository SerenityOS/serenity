/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class StorageDevice : public BlockDevice {
    friend class StorageManagement;
    AK_MAKE_ETERNAL

public:
    virtual u64 max_addressable_block() const { return m_max_addressable_block; }

    NonnullRefPtr<StorageController> controller() const;

    // ^BlockDevice
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(FileDescription const&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_write(FileDescription const&, size_t) const override;

    // ^Device
    virtual mode_t required_mode() const override { return 0600; }

protected:
    StorageDevice(StorageController const&, size_t, u64);
    StorageDevice(StorageController const&, int, int, size_t, u64);
    // ^DiskDevice
    virtual StringView class_name() const override;

private:
    NonnullRefPtr<StorageController> m_storage_controller;
    NonnullRefPtrVector<DiskPartition> m_partitions;
    u64 m_max_addressable_block;
};

}
