/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>
#include <Kernel/Devices/Storage/StorageController.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Devices/Storage/StorageDevicePartition.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/Firmware/DeviceTree/DeviceRecipe.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <LibPartition/PartitionTable.h>

namespace Kernel {

class AHCIController;
class NVMeController;
class StorageManagement {

public:
    StorageManagement();
    void initialize(bool nvme_poll);
    static StorageManagement& the();

    bool determine_boot_device(StringView boot_argument);
    ErrorOr<NonnullRefPtr<VFSRootContext>> create_first_vfs_root_context() const;

    static MinorNumber generate_storage_minor_number();

    static MinorNumber generate_partition_minor_number();

    static u32 generate_controller_id();

    static u32 generate_relative_nvme_controller_id(Badge<NVMeController>);
    static u32 generate_relative_ahci_controller_id(Badge<AHCIController>);
    static u32 generate_relative_sd_controller_id(Badge<SDHostController>);

    void add_device(StorageDevice&);
    void remove_device(StorageDevice&);

    static void add_recipe(DeviceTree::DeviceRecipe<NonnullRefPtr<StorageController>>);

private:
    void enumerate_pci_controllers(bool nvme_poll);
    void enumerate_storage_devices();
    ErrorOr<void> enumerate_device_partitions(StorageDevice&);
    void enumerate_disk_partitions();

    void determine_boot_device_with_partition_uuid();

    void resolve_partition_from_boot_device_parameter(StorageDevice const& chosen_storage_device, StringView boot_device_prefix);
    void determine_boot_device_with_logical_unit_number();
    void determine_block_boot_device();
    void determine_nvme_boot_device();
    void determine_sd_boot_device();
    void determine_ata_boot_device();
    void determine_hardware_relative_boot_device(StringView relative_hardware_prefix, Function<bool(StorageDevice const&)> filter_device_callback);
    Array<unsigned, 3> extract_boot_device_address_parameters(StringView device_prefix);
    Optional<unsigned> extract_boot_device_partition_number_parameter(StringView device_prefix);

    void dump_storage_devices_and_partitions() const;

    ErrorOr<NonnullOwnPtr<Partition::PartitionTable>> try_to_initialize_partition_table(StorageDevice&) const;

    RefPtr<BlockDevice> boot_block_device() const;

    StringView m_boot_argument;
    LockWeakPtr<BlockDevice> m_boot_block_device;
    Vector<NonnullRefPtr<StorageController>> m_controllers;
    IntrusiveList<&StorageDevice::m_list_node> m_storage_devices;
};

}
