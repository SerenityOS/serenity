/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/UUID.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/DeviceFile.h>
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

static Singleton<StorageManagement> s_the;
static Atomic<size_t> s_device_minor_number;

UNMAP_AFTER_INIT StorageManagement::StorageManagement()
{
}

void StorageManagement::enumerate_disk_partitions_on_new_device(StorageDevice& device)
{
    // FIXME: Add proper locking to this...
    VERIFY_INTERRUPTS_ENABLED();
    VERIFY(!device.partitions().size());
    auto partition_table = try_to_initialize_partition_table(device);
    if (!partition_table)
        return;
    for (size_t partition_index = 0; partition_index < partition_table->partitions_count(); partition_index++) {
        auto partition_metadata = partition_table->partition(partition_index);
        if (!partition_metadata.has_value())
            continue;
        // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
        auto disk_partition = DiskPartition::create(const_cast<StorageDevice&>(device), (partition_index + (16 * device.minor())), partition_metadata.value());
        const_cast<StorageDevice&>(device).m_partitions.append(disk_partition);
    }
}

bool StorageManagement::boot_argument_contains_partition_uuid()
{
    return m_boot_argument.starts_with("PARTUUID=");
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_controllers(bool force_pio)
{
    VERIFY(m_controllers.is_empty());
    if (!kernel_command_line().disable_physical_storage()) {
        if (kernel_command_line().is_ide_enabled()) {
            PCI::enumerate([&](const PCI::Address& address, PCI::ID) {
                if (PCI::get_class(address) == PCI_MASS_STORAGE_CLASS_ID && PCI::get_subclass(address) == PCI_IDE_CTRL_SUBCLASS_ID) {
                    m_controllers.append(IDEController::initialize(address, force_pio));
                }
            });
        }
        PCI::enumerate([&](const PCI::Address& address, PCI::ID) {
            if (PCI::get_class(address) == PCI_MASS_STORAGE_CLASS_ID && PCI::get_subclass(address) == PCI_SATA_CTRL_SUBCLASS_ID && PCI::get_programming_interface(address) == PCI_AHCI_IF_PROGIF) {
                m_controllers.append(AHCIController::initialize(address));
            }
        });
    }
    m_controllers.append(RamdiskController::initialize());
}

OwnPtr<PartitionTable> StorageManagement::try_to_initialize_partition_table(const StorageDevice& device)
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

void StorageManagement::determine_boot_device_with_defined_prefix(StringView prefix)
{
    StringView storage_name = m_boot_argument.substring_view(prefix.length());
    auto index_parts = String(storage_name).split(',');
    VERIFY(index_parts.size() <= 2);
    VERIFY(index_parts.size() >= 1);
    size_t storage_device_index = index_parts[0].to_uint(TrimWhitespace::No).value();
    // FIXME: For now, the code that handles the highest count of drives is AHCI
    // so it's 32 ports without port multipliers.
    VERIFY(storage_device_index < 32);
    NonnullRefPtrVector<StorageDevice> devices;
    for (auto& storage_controller : m_controllers) {
        size_t max_devices_count = storage_controller.devices_count();
        if (auto possible_max_devices_count = storage_controller.max_devices_count(); possible_max_devices_count.has_value())
            max_devices_count = possible_max_devices_count.value();

        for (size_t device_index = 0; device_index < max_devices_count; device_index++) {
            StorageAddress address = { static_cast<u8>(device_index), 0, 0 };
            auto device = storage_controller.search_for_device(address);
            if (!device)
                continue;
            devices.append(*device);
        }
    }
    if (!(storage_device_index < devices.size()))
        return;
    if (index_parts.size() == 1) {
        // No partition was specified, try to use a whole StorageDevice
        m_boot_block_device = devices[storage_device_index];
        return;
    }

    size_t partition_index = index_parts[1].to_uint(TrimWhitespace::No).value();
    // Note: We start counting from 0 in partition_index.
    if (devices[storage_device_index].partitions().size() > (partition_index)) {
        m_boot_block_device = devices[storage_device_index].partitions()[partition_index];
    }
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device()
{
    VERIFY(!m_controllers.is_empty());
    if (m_boot_argument.starts_with("hd")) {
        determine_boot_device_with_defined_prefix("hd");
    }

    if (m_boot_block_device.is_null()) {
        PANIC("StorageManagement: boot device {} not found", m_boot_argument);
    }
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device_with_partition_uuid()
{
    VERIFY(m_boot_argument.starts_with("PARTUUID="));

    auto partition_uuid = UUID(m_boot_argument.substring_view(strlen("PARTUUID=")));

    if (partition_uuid.to_string().length() != 36) {
        PANIC("StorageManagement: Specified partition UUID is not valid");
    }
    for (auto& storage_controller : m_controllers) {
        size_t max_devices_count = storage_controller.devices_count();
        if (auto possible_max_devices_count = storage_controller.max_devices_count(); possible_max_devices_count.has_value())
            max_devices_count = possible_max_devices_count.value();

        for (size_t device_index = 0; device_index < max_devices_count; device_index++) {
            auto device = storage_controller.device_by_index(device_index);
            if (!device)
                continue;
            for (auto& partition : device->partitions()) {
                if (partition.metadata().unique_guid().is_zero())
                    continue;
                if (partition.metadata().unique_guid() == partition_uuid) {
                    m_boot_block_device = partition;
                    return;
                }
            }
        }
    }
}

RefPtr<BlockDevice> StorageManagement::boot_block_device() const
{
    return m_boot_block_device.strong_ref();
}

int StorageManagement::major_number()
{
    return 3;
}
int StorageManagement::minor_number()
{
    auto minor_number = s_device_minor_number.load();
    s_device_minor_number++;
    return minor_number;
}

NonnullRefPtr<FileSystem> StorageManagement::root_filesystem() const
{
    if (!boot_block_device()) {
        PANIC("StorageManagement: Couldn't find a suitable device to boot from");
    }
    auto boot_file_or_error = DeviceFile::try_create(*boot_block_device());
    VERIFY(!boot_file_or_error.is_error());

    auto boot_description_or_error = OpenFileDescription::try_create(*boot_file_or_error.release_value());
    VERIFY(!boot_description_or_error.is_error());

    auto file_system = Ext2FS::try_create(boot_description_or_error.release_value()).release_value();

    if (auto result = file_system->initialize(); result.is_error()) {
        PANIC("StorageManagement: Couldn't open root filesystem: {}", result);
    }
    return file_system;
}

UNMAP_AFTER_INIT void StorageManagement::initialize(String root_device, bool force_pio)
{
    VERIFY(s_device_minor_number == 0);
    m_boot_argument = root_device;
    enumerate_controllers(force_pio);
    if (!boot_argument_contains_partition_uuid()) {
        determine_boot_device();
        return;
    }
    determine_boot_device_with_partition_uuid();
}

StorageManagement& StorageManagement::the()
{
    return *s_the;
}

}
