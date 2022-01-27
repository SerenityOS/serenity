/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/UUID.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/Panic.h>
#include <Kernel/Storage/ATA/AHCIController.h>
#include <Kernel/Storage/ATA/IDEController.h>
#include <Kernel/Storage/NVMe/NVMeController.h>
#include <Kernel/Storage/Partition/EBRPartitionTable.h>
#include <Kernel/Storage/Partition/GUIDPartitionTable.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

static Singleton<StorageManagement> s_the;
static Atomic<u32> s_device_minor_number;

static constexpr StringView partition_uuid_prefix = "PARTUUID:"sv;

UNMAP_AFTER_INIT StorageManagement::StorageManagement()
{
}

void StorageManagement::remove_device(StorageDevice& device)
{
    m_storage_devices.remove(device);
}

bool StorageManagement::boot_argument_contains_partition_uuid()
{
    return m_boot_argument.starts_with(partition_uuid_prefix);
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_controllers(bool force_pio, bool nvme_poll)
{
    VERIFY(m_controllers.is_empty());

    using SubclassID = PCI::MassStorage::SubclassID;
    if (!kernel_command_line().disable_physical_storage()) {

        PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
            if (device_identifier.class_code().value() != to_underlying(PCI::ClassID::MassStorage)) {
                return;
            }

            {
                static constexpr PCI::HardwareID vmd_device = { 0x8086, 0x9a0b };
                if (device_identifier.hardware_id() == vmd_device) {
                    auto controller = PCI::VolumeManagementDevice::must_create(device_identifier);
                    PCI::Access::the().add_host_controller_and_enumerate_attached_devices(move(controller), [this, nvme_poll](PCI::DeviceIdentifier const& device_identifier) -> void {
                        auto subclass_code = static_cast<SubclassID>(device_identifier.subclass_code().value());
                        if (subclass_code == SubclassID::NVMeController) {
                            auto controller = NVMeController::try_initialize(device_identifier, nvme_poll);
                            if (controller.is_error()) {
                                dmesgln("Unable to initialize NVMe controller: {}", controller.error());
                            } else {
                                m_controllers.append(controller.release_value());
                            }
                        }
                    });
                }
            }

            auto subclass_code = static_cast<SubclassID>(device_identifier.subclass_code().value());
            if (subclass_code == SubclassID::IDEController && kernel_command_line().is_ide_enabled()) {
                m_controllers.append(IDEController::initialize(device_identifier, force_pio));
            }

            if (subclass_code == SubclassID::SATAController
                && device_identifier.prog_if().value() == to_underlying(PCI::MassStorage::SATAProgIF::AHCI)) {
                m_controllers.append(AHCIController::initialize(device_identifier));
            }
            if (subclass_code == SubclassID::NVMeController) {
                auto controller = NVMeController::try_initialize(device_identifier, nvme_poll);
                if (controller.is_error()) {
                    dmesgln("Unable to initialize NVMe controller: {}", controller.error());
                } else {
                    m_controllers.append(controller.release_value());
                }
            }
        });
    }
    m_controllers.append(RamdiskController::initialize());
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_storage_devices()
{
    VERIFY(!m_controllers.is_empty());
    for (auto& controller : m_controllers) {
        for (size_t device_index = 0; device_index < controller.devices_count(); device_index++) {
            auto device = controller.device(device_index);
            if (device.is_null())
                continue;
            m_storage_devices.append(device.release_nonnull());
        }
    }
}

UNMAP_AFTER_INIT void StorageManagement::dump_storage_devices_and_partitions() const
{
    dbgln("StorageManagement: Detected {} storage devices", m_storage_devices.size_slow());
    for (auto const& storage_device : m_storage_devices) {
        auto const& partitions = storage_device.partitions();
        if (partitions.is_empty()) {
            dbgln("  Device: {} (no partitions)", storage_device.early_storage_name());
        } else {
            dbgln("  Device: {} ({} partitions)", storage_device.early_storage_name(), partitions.size());
            unsigned partition_number = 1;
            for (auto const& partition : partitions) {
                dbgln("    Partition: {} (UUID {})", partition_number, partition.metadata().unique_guid().to_string());
                partition_number++;
            }
        }
    }
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
    if (mbr_table_or_result.error() == PartitionTable::Error::ContainsEBR) {
        auto ebr_table_or_result = EBRPartitionTable::try_to_initialize(device);
        if (ebr_table_or_result.is_error())
            return {};
        return move(ebr_table_or_result.value());
    }
    return {};
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_disk_partitions()
{
    VERIFY(!m_storage_devices.is_empty());
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
            auto disk_partition = DiskPartition::create(device, (partition_index + (16 * device_index)), partition_metadata.value());
            device.add_partition(disk_partition);
        }
        device_index++;
    }
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device()
{
    VERIFY(!m_controllers.is_empty());
    if (m_boot_argument.starts_with("/dev/"sv)) {
        StringView storage_name = m_boot_argument.substring_view(5);
        for (auto& storage_device : m_storage_devices) {
            if (storage_device.early_storage_name() == storage_name) {
                m_boot_block_device = storage_device;
                break;
            }

            // If the early storage name's last character is a digit (e.g. in the case of NVMe where the last
            // number in the device name indicates the node, e.g. /dev/nvme0n1 we need to append a "p" character
            // so that we can properly distinguish the partition index from the device itself
            char storage_name_last_char = *(storage_device.early_storage_name().end() - 1);
            OwnPtr<KString> normalized_name;
            StringView early_storage_name;
            if (storage_name_last_char >= '0' && storage_name_last_char <= '9') {
                normalized_name = MUST(KString::formatted("{}p", storage_device.early_storage_name()));
                early_storage_name = normalized_name->view();
            } else {
                early_storage_name = storage_device.early_storage_name();
            }

            auto start_storage_name = storage_name.substring_view(0, min(early_storage_name.length(), storage_name.length()));

            if (early_storage_name.starts_with(start_storage_name)) {
                StringView partition_sign = storage_name.substring_view(start_storage_name.length());
                auto possible_partition_number = partition_sign.to_uint<size_t>();
                if (!possible_partition_number.has_value())
                    break;
                if (possible_partition_number.value() == 0)
                    break;
                if (storage_device.partitions().size() < possible_partition_number.value())
                    break;
                m_boot_block_device = storage_device.partitions()[possible_partition_number.value() - 1];
                break;
            }
        }
    }

    if (m_boot_block_device.is_null()) {
        dump_storage_devices_and_partitions();
        PANIC("StorageManagement: boot device {} not found", m_boot_argument);
    }
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device_with_partition_uuid()
{
    VERIFY(!m_storage_devices.is_empty());
    VERIFY(m_boot_argument.starts_with(partition_uuid_prefix));

    auto partition_uuid = UUID(m_boot_argument.substring_view(partition_uuid_prefix.length()), UUID::Endianness::Mixed);

    if (partition_uuid.to_string().length() != 36) {
        // FIXME: It would be helpful to output the specified and detected UUIDs in this case,
        //        but we never actually enter this path - if the length doesn't match, the UUID
        //        constructor above crashes with a VERIFY in convert_string_view_to_uuid().
        PANIC("StorageManagement: Specified partition UUID is not valid");
    }
    for (auto& storage_device : m_storage_devices) {
        for (auto& partition : storage_device.partitions()) {
            if (partition.metadata().unique_guid().is_zero())
                continue;
            if (partition.metadata().unique_guid() == partition_uuid) {
                m_boot_block_device = partition;
                break;
            }
        }
    }
}

RefPtr<BlockDevice> StorageManagement::boot_block_device() const
{
    return m_boot_block_device.strong_ref();
}

MajorNumber StorageManagement::storage_type_major_number()
{
    return 3;
}
MinorNumber StorageManagement::generate_storage_minor_number()
{
    auto minor_number = s_device_minor_number.load();
    s_device_minor_number++;
    return minor_number;
}

NonnullRefPtr<FileSystem> StorageManagement::root_filesystem() const
{
    auto boot_device_description = boot_block_device();
    if (!boot_device_description) {
        dump_storage_devices_and_partitions();
        PANIC("StorageManagement: Couldn't find a suitable device to boot from");
    }
    auto description_or_error = OpenFileDescription::try_create(boot_device_description.release_nonnull());
    VERIFY(!description_or_error.is_error());

    auto file_system = Ext2FS::try_create(description_or_error.release_value()).release_value();

    if (auto result = file_system->initialize(); result.is_error()) {
        dump_storage_devices_and_partitions();
        PANIC("StorageManagement: Couldn't open root filesystem: {}", result.error());
    }
    return file_system;
}

UNMAP_AFTER_INIT void StorageManagement::initialize(StringView root_device, bool force_pio, bool poll)
{
    VERIFY(s_device_minor_number == 0);
    m_boot_argument = root_device;
    enumerate_controllers(force_pio, poll);
    enumerate_storage_devices();
    enumerate_disk_partitions();
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
