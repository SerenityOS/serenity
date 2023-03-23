/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/UUID.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/ISABus/IDEController.h>
#    include <Kernel/Arch/x86_64/PCI/IDELegacyModeController.h>
#endif
#if ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/SDHostController.h>
#endif
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Panic.h>
#include <Kernel/Storage/ATA/AHCI/Controller.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Storage/NVMe/NVMeController.h>
#include <Kernel/Storage/Ramdisk/Controller.h>
#include <Kernel/Storage/SD/SDHostController.h>
#include <Kernel/Storage/StorageManagement.h>
#include <LibPartition/EBRPartitionTable.h>
#include <LibPartition/GUIDPartitionTable.h>
#include <LibPartition/MBRPartitionTable.h>

namespace Kernel {

static Singleton<StorageManagement> s_the;
static Atomic<u32> s_storage_device_minor_number;
static Atomic<u32> s_partition_device_minor_number;
static Atomic<u32> s_controller_id;

static Atomic<u32> s_relative_ata_controller_id;
static Atomic<u32> s_relative_nvme_controller_id;
static Atomic<u32> s_relative_sd_controller_id;

static constexpr StringView partition_uuid_prefix = "PARTUUID:"sv;

static constexpr StringView partition_number_prefix = "part"sv;
static constexpr StringView block_device_prefix = "block"sv;

static constexpr StringView ata_device_prefix = "ata"sv;
static constexpr StringView nvme_device_prefix = "nvme"sv;
static constexpr StringView ramdisk_device_prefix = "ramdisk"sv;
static constexpr StringView logical_unit_number_device_prefix = "lun"sv;

UNMAP_AFTER_INIT StorageManagement::StorageManagement()
{
}

u32 StorageManagement::generate_relative_nvme_controller_id(Badge<NVMeController>)
{
    auto controller_id = s_relative_nvme_controller_id.load();
    s_relative_nvme_controller_id++;
    return controller_id;
}

u32 StorageManagement::generate_relative_ata_controller_id(Badge<ATAController>)
{
    auto controller_id = s_relative_ata_controller_id.load();
    s_relative_ata_controller_id++;
    return controller_id;
}

u32 StorageManagement::generate_relative_sd_controller_id(Badge<SDHostController>)
{
    auto controller_id = s_relative_sd_controller_id.load();
    s_relative_sd_controller_id++;
    return controller_id;
}

void StorageManagement::remove_device(StorageDevice& device)
{
    m_storage_devices.remove(device);
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_pci_controllers(bool force_pio, bool nvme_poll)
{
    VERIFY(m_controllers.is_empty());

    using SubclassID = PCI::MassStorage::SubclassID;
    if (!kernel_command_line().disable_physical_storage()) {
        // NOTE: Search for VMD devices before actually searching for storage controllers
        // because the VMD device is only a bridge to such (NVMe) controllers.
        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) -> void {
            constexpr PCI::HardwareID vmd_device = { 0x8086, 0x9a0b };
            if (device_identifier.hardware_id() == vmd_device) {
                auto controller = PCI::VolumeManagementDevice::must_create(device_identifier);
                MUST(PCI::Access::the().add_host_controller_and_scan_for_devices(move(controller)));
            }
        }));

        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) -> void {
            if (device_identifier.class_code().value() != to_underlying(PCI::ClassID::MassStorage)) {
                return;
            }

            auto subclass_code = static_cast<SubclassID>(device_identifier.subclass_code().value());
#if ARCH(X86_64)
            if (subclass_code == SubclassID::IDEController && kernel_command_line().is_ide_enabled()) {
                if (auto ide_controller_or_error = PCIIDELegacyModeController::initialize(device_identifier, force_pio); !ide_controller_or_error.is_error())
                    m_controllers.append(ide_controller_or_error.release_value());
                else
                    dmesgln("Unable to initialize IDE controller: {}", ide_controller_or_error.error());
            }
#elif ARCH(AARCH64)
            (void)force_pio;
            TODO_AARCH64();
#else
#    error Unknown architecture
#endif

            if (subclass_code == SubclassID::SATAController
                && device_identifier.prog_if().value() == to_underlying(PCI::MassStorage::SATAProgIF::AHCI)) {
                if (auto ahci_controller_or_error = AHCIController::initialize(device_identifier); !ahci_controller_or_error.is_error())
                    m_controllers.append(ahci_controller_or_error.value());
                else
                    dmesgln("Unable to initialize AHCI controller: {}", ahci_controller_or_error.error());
            }
            if (subclass_code == SubclassID::NVMeController) {
                auto controller = NVMeController::try_initialize(device_identifier, nvme_poll);
                if (controller.is_error()) {
                    dmesgln("Unable to initialize NVMe controller: {}", controller.error());
                } else {
                    m_controllers.append(controller.release_value());
                }
            }
        }));
    }
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_storage_devices()
{
    VERIFY(!m_controllers.is_empty());
    for (auto& controller : m_controllers) {
        for (size_t device_index = 0; device_index < controller->devices_count(); device_index++) {
            auto device = controller->device(device_index);
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
            dbgln("  Device: block{}:{} (no partitions)", storage_device.major(), storage_device.minor());
        } else {
            dbgln("  Device: block{}:{} ({} partitions)", storage_device.major(), storage_device.minor(), partitions.size());
            unsigned partition_number = 1;
            for (auto const& partition : partitions) {
                dbgln("    Partition: {}, block{}:{} (UUID {})", partition_number, partition->major(), partition->minor(), partition->metadata().unique_guid().to_string());
                partition_number++;
            }
        }
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<Partition::PartitionTable>> StorageManagement::try_to_initialize_partition_table(StorageDevice& device) const
{
    auto mbr_table_or_error = Partition::MBRPartitionTable::try_to_initialize(device);
    if (!mbr_table_or_error.is_error())
        return mbr_table_or_error.release_value();
    auto ebr_table_or_error = Partition::EBRPartitionTable::try_to_initialize(device);
    if (!ebr_table_or_error.is_error()) {
        return ebr_table_or_error.release_value();
    }
    return TRY(Partition::GUIDPartitionTable::try_to_initialize(device));
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_disk_partitions()
{
    VERIFY(!m_storage_devices.is_empty());
    for (auto& device : m_storage_devices) {
        auto partition_table_or_error = try_to_initialize_partition_table(device);
        if (partition_table_or_error.is_error())
            continue;
        auto partition_table = partition_table_or_error.release_value();
        for (size_t partition_index = 0; partition_index < partition_table->partitions_count(); partition_index++) {
            auto partition_metadata = partition_table->partition(partition_index);
            if (!partition_metadata.has_value())
                continue;
            auto disk_partition = DiskPartition::create(device, generate_partition_minor_number(), partition_metadata.value());
            device.add_partition(disk_partition);
        }
    }
}

UNMAP_AFTER_INIT Optional<unsigned> StorageManagement::extract_boot_device_partition_number_parameter(StringView device_prefix)
{
    VERIFY(m_boot_argument.starts_with(device_prefix));
    VERIFY(!m_boot_argument.starts_with(partition_uuid_prefix));
    auto storage_device_relative_address_view = m_boot_argument.substring_view(device_prefix.length());
    auto parameter_view = storage_device_relative_address_view.find_last_split_view(';');
    if (parameter_view == storage_device_relative_address_view)
        return {};
    if (!parameter_view.starts_with(partition_number_prefix)) {
        PANIC("StorageManagement: Invalid root boot parameter.");
    }

    auto parameter_number = parameter_view.substring_view(partition_number_prefix.length()).to_uint<unsigned>();
    if (!parameter_number.has_value()) {
        PANIC("StorageManagement: Invalid root boot parameter.");
    }

    return parameter_number.value();
}

UNMAP_AFTER_INIT Array<unsigned, 3> StorageManagement::extract_boot_device_address_parameters(StringView device_prefix)
{
    VERIFY(!m_boot_argument.starts_with(partition_uuid_prefix));
    Array<unsigned, 3> address_parameters;
    auto parameters_view = m_boot_argument.substring_view(device_prefix.length()).find_first_split_view(';');
    size_t parts_count = 0;
    bool parse_failure = false;
    parameters_view.for_each_split_view(':', SplitBehavior::Nothing, [&](StringView parameter_view) {
        if (parse_failure)
            return;
        if (parts_count > 2)
            return;
        auto parameter_number = parameter_view.to_uint<unsigned>();
        if (!parameter_number.has_value()) {
            parse_failure = true;
            return;
        }
        address_parameters[parts_count] = parameter_number.value();
        parts_count++;
    });

    if (parts_count > 3) {
        dbgln("StorageManagement: Detected {} parts in boot device parameter.", parts_count);
        PANIC("StorageManagement: Invalid root boot parameter.");
    }
    if (parse_failure) {
        PANIC("StorageManagement: Invalid root boot parameter.");
    }

    return address_parameters;
}

UNMAP_AFTER_INIT void StorageManagement::resolve_partition_from_boot_device_parameter(StorageDevice const& chosen_storage_device, StringView boot_device_prefix)
{
    auto possible_partition_number = extract_boot_device_partition_number_parameter(boot_device_prefix);
    if (!possible_partition_number.has_value())
        return;

    auto partition_number = possible_partition_number.value();
    if (chosen_storage_device.partitions().size() <= partition_number)
        PANIC("StorageManagement: Invalid partition number parameter.");
    m_boot_block_device = chosen_storage_device.partitions()[partition_number];
}

UNMAP_AFTER_INIT void StorageManagement::determine_hardware_relative_boot_device(StringView relative_hardware_prefix, Function<bool(StorageDevice const&)> filter_device_callback)
{
    VERIFY(m_boot_argument.starts_with(relative_hardware_prefix));
    auto address_parameters = extract_boot_device_address_parameters(relative_hardware_prefix);

    RefPtr<StorageDevice> chosen_storage_device;

    for (auto& storage_device : m_storage_devices) {
        if (!filter_device_callback(storage_device))
            continue;
        auto storage_device_lun = storage_device.logical_unit_number_address();
        if (storage_device.parent_controller_hardware_relative_id() == address_parameters[0]
            && storage_device_lun.target_id == address_parameters[1]
            && storage_device_lun.disk_id == address_parameters[2]) {
            m_boot_block_device = storage_device;
            chosen_storage_device = storage_device;
            break;
        }
    }

    if (chosen_storage_device)
        resolve_partition_from_boot_device_parameter(*chosen_storage_device, relative_hardware_prefix);
}

UNMAP_AFTER_INIT void StorageManagement::determine_ata_boot_device()
{
    determine_hardware_relative_boot_device(ata_device_prefix, [](StorageDevice const& device) -> bool {
        return device.command_set() == StorageDevice::CommandSet::ATA;
    });
}

UNMAP_AFTER_INIT void StorageManagement::determine_nvme_boot_device()
{
    determine_hardware_relative_boot_device(nvme_device_prefix, [](StorageDevice const& device) -> bool {
        return device.command_set() == StorageDevice::CommandSet::NVMe;
    });
}

UNMAP_AFTER_INIT void StorageManagement::determine_ramdisk_boot_device()
{
    determine_hardware_relative_boot_device(ramdisk_device_prefix, [](StorageDevice const& device) -> bool {
        return device.command_set() == StorageDevice::CommandSet::PlainMemory;
    });
}

UNMAP_AFTER_INIT void StorageManagement::determine_block_boot_device()
{
    VERIFY(m_boot_argument.starts_with(block_device_prefix));
    auto parameters_view = extract_boot_device_address_parameters(block_device_prefix);

    // Note: We simply fetch the corresponding BlockDevice with the major and minor parameters.
    // We don't try to accept and resolve a partition number as it will make this code much more
    // complicated. This rule is also explained in the boot_device_addressing(7) manual page.
    LockRefPtr<Device> device = DeviceManagement::the().get_device(parameters_view[0], parameters_view[1]);
    if (device && device->is_block_device())
        m_boot_block_device = static_ptr_cast<BlockDevice>(device);
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device_with_logical_unit_number()
{
    VERIFY(m_boot_argument.starts_with(logical_unit_number_device_prefix));
    auto address_parameters = extract_boot_device_address_parameters(logical_unit_number_device_prefix);

    RefPtr<StorageDevice> chosen_storage_device;

    for (auto& storage_device : m_storage_devices) {
        auto storage_device_lun = storage_device.logical_unit_number_address();
        if (storage_device_lun.controller_id == address_parameters[0]
            && storage_device_lun.target_id == address_parameters[1]
            && storage_device_lun.disk_id == address_parameters[2]) {
            m_boot_block_device = storage_device;
            chosen_storage_device = storage_device;
            break;
        }
    }

    if (chosen_storage_device)
        resolve_partition_from_boot_device_parameter(*chosen_storage_device, logical_unit_number_device_prefix);
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device()
{
    VERIFY(!m_controllers.is_empty());

    if (m_boot_argument.starts_with(block_device_prefix)) {
        determine_block_boot_device();
        return;
    }

    if (m_boot_argument.starts_with(partition_uuid_prefix)) {
        determine_boot_device_with_partition_uuid();
        return;
    }

    if (m_boot_argument.starts_with(logical_unit_number_device_prefix)) {
        determine_boot_device_with_logical_unit_number();
        return;
    }

    if (m_boot_argument.starts_with(ata_device_prefix)) {
        determine_ata_boot_device();
        return;
    }

    if (m_boot_argument.starts_with(ramdisk_device_prefix)) {
        determine_ramdisk_boot_device();
        return;
    }

    if (m_boot_argument.starts_with(nvme_device_prefix)) {
        determine_nvme_boot_device();
        return;
    }
    PANIC("StorageManagement: Invalid root boot parameter.");
}

UNMAP_AFTER_INIT void StorageManagement::determine_boot_device_with_partition_uuid()
{
    VERIFY(!m_storage_devices.is_empty());
    VERIFY(m_boot_argument.starts_with(partition_uuid_prefix));

    auto partition_uuid = UUID(m_boot_argument.substring_view(partition_uuid_prefix.length()), UUID::Endianness::Mixed);

    for (auto& storage_device : m_storage_devices) {
        for (auto& partition : storage_device.partitions()) {
            if (partition->metadata().unique_guid().is_zero())
                continue;
            if (partition->metadata().unique_guid() == partition_uuid) {
                m_boot_block_device = partition;
                break;
            }
        }
    }
}

LockRefPtr<BlockDevice> StorageManagement::boot_block_device() const
{
    return m_boot_block_device.strong_ref();
}

MajorNumber StorageManagement::storage_type_major_number()
{
    return 3;
}
MinorNumber StorageManagement::generate_storage_minor_number()
{
    return s_storage_device_minor_number.fetch_add(1);
}

MinorNumber StorageManagement::generate_partition_minor_number()
{
    return s_partition_device_minor_number.fetch_add(1);
}

u32 StorageManagement::generate_controller_id()
{
    return s_controller_id.fetch_add(1);
}

NonnullLockRefPtr<FileSystem> StorageManagement::root_filesystem() const
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
    VERIFY(s_storage_device_minor_number == 0);
    m_boot_argument = root_device;
    if (PCI::Access::is_disabled()) {
#if ARCH(X86_64)
        // Note: If PCI is disabled, we assume that at least we have an ISA IDE controller
        // to probe and use
        auto isa_ide_controller = MUST(ISAIDEController::initialize());
        m_controllers.append(isa_ide_controller);
#endif
    } else {
        enumerate_pci_controllers(force_pio, poll);
    }

#if ARCH(AARCH64)
    auto& rpi_sdhc = RPi::SDHostController::the();
    if (auto maybe_error = rpi_sdhc.initialize(); maybe_error.is_error()) {
        dmesgln("Unable to initialize RaspberryPi's SD Host Controller: {}", maybe_error.error());
    } else {
        m_controllers.append(rpi_sdhc);
    }
#endif

    // Note: Whether PCI bus is present on the system or not, always try to attach
    // a given ramdisk.
    auto controller = RamdiskController::try_initialize();
    if (controller.is_error()) {
        dmesgln("Unable to initialize RAM controller: {}", controller.error());
    } else {
        m_controllers.append(controller.release_value());
    }
    enumerate_storage_devices();
    enumerate_disk_partitions();

    determine_boot_device();
    if (m_boot_block_device.is_null()) {
        dump_storage_devices_and_partitions();
        PANIC("StorageManagement: boot device {} not found", m_boot_argument);
    }
}

StorageManagement& StorageManagement::the()
{
    return *s_the;
}

}
