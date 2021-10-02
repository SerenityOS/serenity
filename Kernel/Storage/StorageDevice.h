/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class StorageDevice : public BlockDevice {
    friend class StorageManagement;

public:
    virtual u64 max_addressable_block() const { return m_max_addressable_block; }

    NonnullRefPtr<StorageController> controller() const;

    // ^BlockDevice
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual void prepare_for_unplug() { m_partitions.clear(); }

    StringView storage_name() const;
    NonnullRefPtrVector<DiskPartition> partitions() const { return m_partitions; }

protected:
    StorageDevice(const StorageController&, size_t, u64, NonnullOwnPtr<KString>);
    StorageDevice(const StorageController&, int, int, size_t, u64, NonnullOwnPtr<KString>);
    // ^DiskDevice
    virtual StringView class_name() const override;

private:
    mutable IntrusiveListNode<StorageDevice, RefPtr<StorageDevice>> m_list_node;
    NonnullRefPtr<StorageController> m_storage_controller;
    NonnullRefPtrVector<DiskPartition> m_partitions;
    NonnullOwnPtr<KString> m_storage_device_name;
    u64 m_max_addressable_block;
};

}
