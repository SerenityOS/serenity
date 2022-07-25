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
#include <Kernel/Storage/DiskPartition.h>
#include <Kernel/Storage/StorageController.h>

namespace Kernel {

class StorageDevice : public BlockDevice {
    friend class StorageManagement;
    friend class DeviceManagement;

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

    // Note: this attribute describes the interface type of a Storage device.
    // For example, an ordinary harddrive utilizes the ATA command set, while
    // an ATAPI device (e.g. Optical drive) that is connected to the ATA bus,
    // is actually using SCSI commands (packets) encapsulated inside an ATA command.
    // Therefore, an ATAPI device is still using the ATA interface.
    enum class InterfaceType {
        PlainMemory,
        SCSI,
        ATA,
        NVMe,
    };

    // Note: The most reliable way to address this device from userspace interfaces,
    // such as SysFS, is to have one way to enumerate everything in the eyes of userspace.
    // Therefore, SCSI LUN (logical unit number) addressing seem to be the most generic way to do this.
    // For example, on a legacy ATA instance, one might connect an harddrive to the second IDE controller,
    // to the Primary channel as a slave device, which translates to LUN 1:0:1.
    // On NVMe, for example, connecting a second PCIe NVMe storage device as a sole NVMe namespace translates
    // to LUN 1:0:0.
    // TODO: LUNs are also useful also when specifying the boot drive on boot. Consider doing that.
    struct LUNAddress {
        u32 controller_id;
        u32 target_id;
        u32 disk_id;
    };

public:
    virtual u64 max_addressable_block() const { return m_max_addressable_block; }

    // ^BlockDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual void prepare_for_unplug() { m_partitions.clear(); }

    // FIXME: Remove this method after figuring out another scheme for naming.
    StringView early_storage_name() const;

    NonnullRefPtrVector<DiskPartition> const& partitions() const { return m_partitions; }

    void add_partition(NonnullRefPtr<DiskPartition> disk_partition) { MUST(m_partitions.try_append(disk_partition)); }

    LUNAddress const& logical_unit_number_address() const { return m_logical_unit_number_address; }

    virtual CommandSet command_set() const = 0;

    StringView interface_type_to_string_view() const;
    StringView command_set_to_string_view() const;

    // ^File
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) final;

protected:
    StorageDevice(LUNAddress, MajorNumber, MinorNumber, size_t, u64, NonnullOwnPtr<KString>);
    // ^DiskDevice
    virtual StringView class_name() const override;

private:
    virtual void after_inserting() override;
    virtual void will_be_destroyed() override;

    virtual InterfaceType interface_type() const = 0;
    mutable IntrusiveListNode<StorageDevice, RefPtr<StorageDevice>> m_list_node;
    NonnullRefPtrVector<DiskPartition> m_partitions;

    // FIXME: Remove this method after figuring out another scheme for naming.
    NonnullOwnPtr<KString> m_early_storage_device_name;
    LUNAddress const m_logical_unit_number_address;
    u64 m_max_addressable_block { 0 };
    size_t m_blocks_per_page { 0 };
};

}
