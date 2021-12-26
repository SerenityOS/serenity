/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/UUID.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/Panic.h>
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/Partition/EBRPartitionTable.h>
#include <Kernel/Storage/Partition/GUIDPartitionTable.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

static StorageManagement* s_the;
static size_t s_device_minor_number;

UNMAP_AFTER_INIT StorageManagement::StorageManagement(String boot_argument, bool force_pio)
    : m_boot_argument(boot_argument)
    , m_controllers(enumerate_controllers(force_pio))
    , m_storage_devices(enumerate_storage_devices())
    , m_disk_partitions(enumerate_disk_partitions())
{
    s_device_minor_number = 0;
    if (!boot_argument_contains_partition_uuid()) {
        determine_boot_device();
        return;
    }
    determine_boot_device_with_partition_uuid();
}

bool StorageManagement::boot_argument_contains_partition_uuid()
{
    return m_boot_argument.starts_with("PARTUUID=");
}

UNMAP_AFTER_INIT NonnullRefPtrVector<StorageController> StorageManagement::enumerate_controllers(bool force_pio) const
{
    NonnullRefPtrVector<StorageController> controllers;
    if (!kernel_command_line().disable_physical_storage()) {
        if (kernel_command_line().is_ide_enabled()) {
            PCI::enumerate([&](const PCI::Address& address, PCI::ID) {
                if (PCI::get_class(address) == 0x1 && PCI::get_subclass(address) == 0x1) {
                    controllers.append(IDEController::initialize(address, force_pio));
                }
            });
        }
        PCI::enumerate([&](const PCI::Address& address, PCI::ID) {
            if (PCI::get_class(address) == 0x1 && PCI::get_subclass(address) == 0x6 && PCI::get_programming_interface(address) == 0x1) {
                controllers.append(AHCIController::initialize(address));
            }
        });
    }
    controllers.append(RamdiskController::initialize());
    return controllers;
}

UNMAP_AFTER_INIT NonnullRefPtrVector<StorageDevice> StorageManagement::enumerate_storage_devices() const
{
    VERIFY(!m_controllers.is_empty());
    NonnullRefPtrVector<StorageDevice> devices;
    for (auto& controller : m_controllers) {
        for (size_t device_index = 0; device_index < controller.devices_count(); device_index++) {
            auto device = controller.device(device_index);
            if (device.is_null())
                continue;
            devices.append(device.release_nonnull());
        }
    }
    return devices;
}

UNMAP_AFTER_INIT OwnPtr<PartitionTable> StorageManagement::try_to_initialize_partition_table(const StorageDevice& device) const
{
    auto mbr_table_or_result = MBRPartitionTable::try_to_initialize(device);
    if (!mbr_table_or_result.is_error())
        return move(mbr_table_or_result.value());
    if (mbr_table_or_result.error() == PartitionTable::Error::MBRProtective) {
        auto gpt_table_or_result = GUIDPartitionTable::try_to_initialize(device);
        if (gpt_table_or_result.is_error())
            return {};
        return move(gpt_table_or_result.value());
    }
    if (mbr_table_or_result.error() == PartitionTable::Error::ConatinsEBR) {
        auto ebr_table_or_result = EBRPartitionTable::try_to_initialize(device);
        if (ebr_table_or_result.is_error())
            return {};
        return move(ebr_table_or_result.value());
    }
    return {};
}

UNMAP_AFTER_INIT NonnullRefPtrVector<DiskPartition> StorageManagement::enumerate_disk_partitions() const
{
    VERIFY(!m_storage_devices.is_empty());
    NonnullRefPtrVector<DiskPartition> partitions;
    size_t device_index = 0;
    for (auto& device : m_storage_devices) {
        auto partition_table = try_to_initialize_partition_table(device);
        if (!partition_table)
            continue;
        for (size_t partition_index = 0; partition_index < partition_table->partitions_count(); partition_index++) {
            auto partition_metadata = partition_table->partition(partition_index);
            if (!partition_metadata.has_value())
                continue;
            // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
            auto disk_partition = DiskPartition::create(const_cast<StorageDevice&>(device), (partition_index + (16 * device_index)), partition_metadata.value());
            partitions.append(disk_partition);
            const_cast<StorageDevice&>(device).m_partitions.append(disk_partition);
        }
        device_index++;
    }
    return partitions;
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device()
{
    VERIFY(!m_controllers.is_empty());
    if (m_boot_argument.starts_with("/dev/")) {
        StringView device_name = m_boot_argument.substring_view(5);
        Device::for_each([&](Device& device) {
            if (device.is_block_device()) {
                auto& block_device = static_cast<BlockDevice&>(device);
                if (device.device_name() == device_name) {
                    m_boot_block_device = block_device;
                }
            }
        });
    }

    if (m_boot_block_device.is_null()) {
        PANIC("StorageManagement: boot device {} not found", m_boot_argument);
    }
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device_with_partition_uuid()
{
    VERIFY(!m_disk_partitions.is_empty());
    VERIFY(m_boot_argument.starts_with("PARTUUID="));

    auto partition_uuid = UUID(m_boot_argument.substring_view(strlen("PARTUUID=")));

    if (partition_uuid.to_string().length() != 36) {
        PANIC("StorageManagement: Specified partition UUID is not valid");
    }

    for (auto& partition : m_disk_partitions) {
        if (partition.metadata().unique_guid().is_zero())
            continue;
        if (partition.metadata().unique_guid() == partition_uuid) {
            m_boot_block_device = partition;
            break;
        }
    }
}

RefPtr<BlockDevice> StorageManagement::boot_block_device() const
{
    return m_boot_block_device;
}

int StorageManagement::major_number()
{
    return 3;
}
int StorageManagement::minor_number()
{
    return s_device_minor_number++;
}

NonnullRefPtr<FileSystem> StorageManagement::root_filesystem() const
{
    auto boot_device_description = boot_block_device();
    if (!boot_device_description) {
        PANIC("StorageManagement: Couldn't find a suitable device to boot from");
    }
    auto e2fs = Ext2FS::create(FileDescription::create(boot_device_description.release_nonnull()).value());
    if (!e2fs->initialize()) {
        PANIC("StorageManagement: Couldn't open root filesystem");
    }
    return e2fs;
}

bool StorageManagement::initialized()
{
    return (s_the != nullptr);
}

UNMAP_AFTER_INIT void StorageManagement::initialize(String root_device, bool force_pio)
{
    VERIFY(!StorageManagement::initialized());
    s_the = new StorageManagement(root_device, force_pio);
}

StorageManagement& StorageManagement::the()
{
    return *s_the;
}

}
