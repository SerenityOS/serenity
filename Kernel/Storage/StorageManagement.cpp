/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IterationDecision.h>
#include <AK/Platform.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/UUID.h>
#if ARCH(I386) || ARCH(X86_64)
#    include <Kernel/Arch/x86/ISABus/IDEController.h>
#    include <Kernel/Arch/x86/PCI/IDELegacyModeController.h>
#endif
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Panic.h>
#include <Kernel/Storage/ATA/AHCI/Controller.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Storage/NVMe/NVMeController.h>
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

static constexpr StringView partition_uuid_prefix = "PARTUUID:"sv;

static constexpr StringView partition_number_prefix = "part"sv;
static constexpr StringView block_device_prefix = "block"sv;

static constexpr StringView ata_device_prefix = "ata"sv;
static constexpr StringView nvme_device_prefix = "nvme"sv;
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

void StorageManagement::remove_device(StorageDevice& device)
{
    m_storage_devices.remove(device);
}

UNMAP_AFTER_INIT void StorageManagement::enumerate_pci_controllers(bool force_pio, bool nvme_poll)
{
    VERIFY(m_controllers.is_empty());

    using SubclassID = PCI::MassStorage::SubclassID;
    if (!kernel_command_line().disable_physical_storage()) {

        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) -> void {
            if (device_identifier.class_code().value() != to_underlying(PCI::ClassID::MassStorage)) {
                return;
            }

            {
                constexpr PCI::HardwareID vmd_device = { 0x8086, 0x9a0b };
                if (device_identifier.hardware_id() == vmd_device) {
                    auto controller = PCI::VolumeManagementDevice::must_create(device_identifier);
                    MUST(PCI::Access::the().add_host_controller_and_enumerate_attached_devices(move(controller), [this, nvme_poll](PCI::DeviceIdentifier const& device_identifier) -> void {
                        auto subclass_code = static_cast<SubclassID>(device_identifier.subclass_code().value());
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

            auto subclass_code = static_cast<SubclassID>(device_identifier.subclass_code().value());
#if ARCH(I386) || ARCH(X86_64)
            if (subclass_code == SubclassID::IDEController && kernel_command_line().is_ide_enabled()) {
                m_controllers.append(PCIIDELegacyModeController::initialize(device_identifier, force_pio));
            }
#elif ARCH(AARCH64)
            (void)force_pio;
            TODO_AARCH64();
#else
#    error Unknown architecture
#endif

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
        }));
    }
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
            dbgln("  Device: block{}:{} (no partitions)", storage_device.major(), storage_device.minor());
        } else {
            dbgln("  Device: block{}:{} ({} partitions)", storage_device.major(), storage_device.minor(), partitions.size());
            unsigned partition_number = 1;
            for (auto const& partition : partitions) {
                dbgln("    Partition: {}, block{}:{} (UUID {})", partition_number, partition.major(), partition.minor(), partition.metadata().unique_guid().to_string());
                partition_number++;
            }
        }
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<Partition::PartitionTable>> StorageManagement::try_to_initialize_partition_table(StorageDevice const& device) const
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

UNMAP_AFTER_INIT void StorageManagement::initialize(PhysicalAddress initramfs_start, PhysicalAddress initramfs_end, bool force_pio, bool poll)
{
    VERIFY(s_storage_device_minor_number == 0);
    if (PCI::Access::is_disabled()) {
#if ARCH(I386) || ARCH(X86_64)
        // Note: If PCI is disabled, we assume that at least we have an ISA IDE controller
        // to probe and use
        m_controllers.append(ISAIDEController::initialize());
#endif
    } else {
        enumerate_pci_controllers(force_pio, poll);
    }
    enumerate_storage_devices();
    enumerate_disk_partitions();

    auto initramfs = MUST(initialize_initramfs(initramfs_start, initramfs_end));
    if (VirtualFileSystem::the().mount_root(*initramfs).is_error()) {
        PANIC("VirtualFileSystem::mount_root failed");
    }
}

static ErrorOr<u32> convert_to_uint_from_inode_field(StringView octal_number)
{
    auto number_or_error = AK::StringUtils::convert_to_uint_from_octal<u32>(octal_number);
    if (!number_or_error.has_value())
        return Error::from_errno(EINVAL);
    return number_or_error.value();
}

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<TmpFSInode>> StorageManagement::try_create_tmpfs_inode_for_initramfs(TmpFS& fs, PhysicalAddress current_address, TmpFSInode const& parent_directory_inode, InitRAMFSInodeHeader const& header)
{
    InodeMetadata metadata;
    auto inode_number = TRY(convert_to_uint_from_inode_field(StringView { header.inode_number, sizeof(InitRAMFSInodeHeader::inode_number) }));
    auto filename_length = TRY(convert_to_uint_from_inode_field(StringView { header.filename_length, sizeof(InitRAMFSInodeHeader::filename_length) }));
    metadata.inode = InodeIdentifier { fs.fsid(), inode_number };

    auto file_size = TRY(convert_to_uint_from_inode_field(StringView { header.file_size, sizeof(InitRAMFSInodeHeader::file_size) }));
    metadata.size = file_size;
    auto mode = TRY(convert_to_uint_from_inode_field(StringView { header.mode, sizeof(InitRAMFSInodeHeader::mode) }));
    metadata.mode = mode;

    metadata.uid = TRY(convert_to_uint_from_inode_field(StringView { header.uid, sizeof(InitRAMFSInodeHeader::uid) }));
    metadata.gid = TRY(convert_to_uint_from_inode_field(StringView { header.gid, sizeof(InitRAMFSInodeHeader::gid) }));
    metadata.link_count = TRY(convert_to_uint_from_inode_field(StringView { header.nlink, sizeof(InitRAMFSInodeHeader::nlink) }));
    metadata.atime = TRY(convert_to_uint_from_inode_field(StringView { header.mtime, sizeof(InitRAMFSInodeHeader::mtime) }));
    metadata.ctime = TRY(convert_to_uint_from_inode_field(StringView { header.mtime, sizeof(InitRAMFSInodeHeader::mtime) }));
    metadata.mtime = TRY(convert_to_uint_from_inode_field(StringView { header.mtime, sizeof(InitRAMFSInodeHeader::mtime) }));
    metadata.dtime = TRY(convert_to_uint_from_inode_field(StringView { header.mtime, sizeof(InitRAMFSInodeHeader::mtime) }));
    metadata.block_size = PAGE_SIZE;
    // FIXME: Is this correct in CPIO archives?
    metadata.block_count = static_cast<blkcnt_t>(static_cast<u64>(metadata.size) / static_cast<u64>(PAGE_SIZE));

    if (Kernel::is_character_device(mode) || Kernel::is_block_device(mode)) {
        auto dev = TRY(convert_to_uint_from_inode_field(StringView { header.rdev, sizeof(header.rdev) }));
        metadata.major_device = (dev & 0xfff00) >> 8;
        metadata.minor_device = (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
    if (metadata.is_directory()) {
        return TmpFSInode::try_create_as_directory({}, fs, metadata, parent_directory_inode);
    }

    if (file_size == 0) {
        return TmpFSInode::try_create_with_empty_content({}, fs, metadata, parent_directory_inode);
    }

    auto mapping_size = TRY(Memory::page_round_up(sizeof(InitRAMFSInodeHeader) + filename_length + file_size));
    auto mapping = TRY(Memory::map_typed<u8>(current_address, mapping_size));
    auto mapped_buffer = Span<u8> { mapping.base_address().offset(sizeof(InitRAMFSInodeHeader) + filename_length).as_ptr(), file_size };
    return TmpFSInode::try_create_with_content({}, fs, metadata, mapped_buffer, parent_directory_inode);
}

ErrorOr<NonnullLockRefPtr<TmpFSInode>> StorageManagement::ensure_initramfs_path(TmpFSInode& inode, StringView full_name)
{
    auto first_path_part = full_name.find_first_split_view('/');
    if (first_path_part == full_name)
        return inode;
    auto result = inode.lookup(first_path_part);
    if (result.is_error()) {
        VERIFY(result.error() != Error::from_errno(ENOENT));
        return result.release_error();
    }
    auto next_inode = static_ptr_cast<TmpFSInode>(result.release_value());
    return ensure_initramfs_path(*next_inode, full_name.substring_view(1 + first_path_part.length()));
}

UNMAP_AFTER_INIT ErrorOr<void> StorageManagement::populate_initramfs(TmpFS& fs, PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end)
{
    auto& root_inode = static_cast<TmpFSInode&>(fs.root_inode());
    auto current_address = initramfs_image_start;
    while (current_address < initramfs_image_end) {
        if (initramfs_image_end.get() - current_address.get() < sizeof(InitRAMFSInodeHeader))
            break;
        if (initramfs_image_end.get() - current_address.get() < sizeof(InitRAMFSInodeHeader))
            break;
        auto mapping = TRY(Memory::map_typed<InitRAMFSInodeHeader>(current_address));
        auto magic = StringView { mapping->magic, sizeof(InitRAMFSInodeHeader::magic) };
        VERIFY(magic == "070707"sv);
        auto file_name_length = TRY(convert_to_uint_from_inode_field(StringView { mapping->filename_length, sizeof(InitRAMFSInodeHeader::filename_length) }));
        auto mapping_with_name = TRY(Memory::map_typed<InitRAMFSRegularInodeHeader>(current_address, sizeof(InitRAMFSInodeHeader) + file_name_length));
        auto name = StringView { mapping_with_name->name, file_name_length - 1 };
        if (name == "TRAILER!!!"sv)
            break;
        auto mode = TRY(convert_to_uint_from_inode_field(StringView { mapping->mode, sizeof(InitRAMFSInodeHeader::mode) }));
        auto file_size = TRY(convert_to_uint_from_inode_field(StringView { mapping->file_size, sizeof(InitRAMFSInodeHeader::file_size) }));
        auto parent_directory_inode = TRY(ensure_initramfs_path(root_inode, name));
        auto new_inode = TRY(try_create_tmpfs_inode_for_initramfs(fs, current_address, parent_directory_inode, *mapping.ptr()));
        auto basename = name.find_last_split_view('/');
        TRY(parent_directory_inode->add_child(new_inode, basename, mode));
        current_address = current_address.offset(sizeof(InitRAMFSInodeHeader) + file_name_length + file_size);
    }
    return {};
}

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<TmpFS>> StorageManagement::initialize_initramfs(PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end)
{
    auto fs = static_ptr_cast<TmpFS>(TRY(TmpFS::try_create()));
    TRY(fs->initialize());
    TRY(populate_initramfs(*fs, initramfs_image_start, initramfs_image_end));
    return fs;
}

StorageManagement& StorageManagement::the()
{
    return *s_the;
}

}
