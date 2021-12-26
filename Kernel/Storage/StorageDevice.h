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
    // Note: this attribute describes the internal command set of a Storage device.
    // For example, an ordinary harddrive utilizes the ATA command set, while
    // an ATAPI device (e.g. Optical drive) that is connected to the ATA bus,
    // is actually using SCSI commands (packets) encapsulated inside an ATA command.
    // The IDE controller code being aware of the possibility of ATAPI devices attached
    // to the ATA bus, will check whether the Command set is ATA or SCSI and will act
    // accordingly.
    enum class CommandSet {
        PlainMemory,
        SCSI,
        ATA,
        NVMe,
    };

public:
    virtual u64 max_addressable_block() const { return m_max_addressable_block; }

    // ^BlockDevice
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual void prepare_for_unplug() { m_partitions.clear(); }

    // FIXME: Remove this method after figuring out another scheme for naming.
    StringView early_storage_name() const;

    NonnullRefPtrVector<DiskPartition> partitions() const { return m_partitions; }

    virtual CommandSet command_set() const = 0;

    // ^File
    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) final;

protected:
    StorageDevice(int, int, size_t, u64, NonnullOwnPtr<KString>);
    // ^DiskDevice
    virtual StringView class_name() const override;

private:
    mutable IntrusiveListNode<StorageDevice, RefPtr<StorageDevice>> m_list_node;
    NonnullRefPtrVector<DiskPartition> m_partitions;

    // FIXME: Remove this method after figuring out another scheme for naming.
    NonnullOwnPtr<KString> m_early_storage_device_name;
    u64 m_max_addressable_block;
};

}
