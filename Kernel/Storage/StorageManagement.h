/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/TmpFS/FileSystem.h>
#include <Kernel/FileSystem/TmpFS/Inode.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
#include <Kernel/Storage/DiskPartition.h>
#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageDevice.h>
#include <LibPartition/PartitionTable.h>

namespace Kernel {

class ATAController;
class NVMeController;
class StorageManagement {

private:
    static_assert(sizeof(char) == sizeof(u8));
    struct [[gnu::packed]] InitRAMFSInodeHeader {
        char magic[6]; // Should be a string of "070707"
        char dev[6];
        char inode_number[6];
        char mode[6];
        char uid[6];
        char gid[6];
        char nlink[6];
        char rdev[6];
        char mtime[11];
        char filename_length[6];
        char file_size[11];
    };

    struct [[gnu::packed]] InitRAMFSRegularInodeHeader {
        InitRAMFSInodeHeader header;
        char name[];
    };

    struct [[gnu::packed]] InitRAMFSTrailerInodeHeader {
        InitRAMFSInodeHeader header;
        char trailer_name[11];
    };

public:
    StorageManagement();
    static bool initialized();
    void initialize(PhysicalAddress initramfs_start, PhysicalAddress initramfs_end, bool force_pio, bool nvme_poll);
    static StorageManagement& the();

    static MajorNumber storage_type_major_number();
    static MinorNumber generate_storage_minor_number();

    static MinorNumber generate_partition_minor_number();

    static u32 generate_controller_id();

    static u32 generate_relative_nvme_controller_id(Badge<NVMeController>);
    static u32 generate_relative_ata_controller_id(Badge<ATAController>);

    void remove_device(StorageDevice&);

private:
    bool boot_argument_contains_partition_uuid();

    void enumerate_pci_controllers(bool force_pio, bool nvme_poll);
    void enumerate_storage_devices();
    void enumerate_disk_partitions();

    void determine_boot_device();
    void determine_boot_device_with_partition_uuid();

    void resolve_partition_from_boot_device_parameter(StorageDevice const& chosen_storage_device, StringView boot_device_prefix);
    void determine_boot_device_with_logical_unit_number();
    void determine_block_boot_device();
    void determine_nvme_boot_device();
    void determine_ata_boot_device();
    void determine_hardware_relative_boot_device(StringView relative_hardware_prefix, Function<bool(StorageDevice const&)> filter_device_callback);
    Array<unsigned, 3> extract_boot_device_address_parameters(StringView device_prefix);
    Optional<unsigned> extract_boot_device_partition_number_parameter(StringView device_prefix);

    void dump_storage_devices_and_partitions() const;

    ErrorOr<NonnullLockRefPtr<TmpFSInode>> try_create_tmpfs_inode_for_initramfs(TmpFS& fs, PhysicalAddress current_address, TmpFSInode const& parent_directory_inode, InitRAMFSInodeHeader const& header);
    ErrorOr<NonnullLockRefPtr<TmpFSInode>> ensure_initramfs_path(TmpFSInode& inode, StringView full_name);
    ErrorOr<void> populate_initramfs(TmpFS& fs, PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end);
    ErrorOr<NonnullLockRefPtr<TmpFS>> initialize_initramfs(PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end);

    ErrorOr<NonnullOwnPtr<Partition::PartitionTable>> try_to_initialize_partition_table(StorageDevice const&) const;

    StringView m_boot_argument;
    NonnullLockRefPtrVector<StorageController> m_controllers;
    IntrusiveList<&StorageDevice::m_list_node> m_storage_devices;
};

}
