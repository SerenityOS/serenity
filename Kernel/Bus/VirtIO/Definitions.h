/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::VirtIO {

#define REG_DEVICE_FEATURES 0x0
#define REG_GUEST_FEATURES 0x4
#define REG_QUEUE_ADDRESS 0x8
#define REG_QUEUE_SIZE 0xc
#define REG_QUEUE_SELECT 0xe
#define REG_QUEUE_NOTIFY 0x10
#define REG_DEVICE_STATUS 0x12
#define REG_ISR_STATUS 0x13

#define DEVICE_STATUS_ACKNOWLEDGE (1 << 0)
#define DEVICE_STATUS_DRIVER (1 << 1)
#define DEVICE_STATUS_DRIVER_OK (1 << 2)
#define DEVICE_STATUS_FEATURES_OK (1 << 3)
#define DEVICE_STATUS_DEVICE_NEEDS_RESET (1 << 6)
#define DEVICE_STATUS_FAILED (1 << 7)

#define VIRTIO_F_INDIRECT_DESC ((u64)1 << 28)
#define VIRTIO_F_VERSION_1 ((u64)1 << 32)
#define VIRTIO_F_RING_PACKED ((u64)1 << 34)
#define VIRTIO_F_IN_ORDER ((u64)1 << 35)

#define VIRTIO_PCI_CAP_COMMON_CFG 1
#define VIRTIO_PCI_CAP_NOTIFY_CFG 2
#define VIRTIO_PCI_CAP_ISR_CFG 3
#define VIRTIO_PCI_CAP_DEVICE_CFG 4
#define VIRTIO_PCI_CAP_PCI_CFG 5

// virtio_pci_common_cfg
#define COMMON_CFG_DEVICE_FEATURE_SELECT 0x0
#define COMMON_CFG_DEVICE_FEATURE 0x4
#define COMMON_CFG_DRIVER_FEATURE_SELECT 0x8
#define COMMON_CFG_DRIVER_FEATURE 0xc
#define COMMON_CFG_MSIX_CONFIG 0x10
#define COMMON_CFG_NUM_QUEUES 0x12
#define COMMON_CFG_DEVICE_STATUS 0x14
#define COMMON_CFG_CONFIG_GENERATION 0x15
#define COMMON_CFG_QUEUE_SELECT 0x16
#define COMMON_CFG_QUEUE_SIZE 0x18
#define COMMON_CFG_QUEUE_MSIX_VECTOR 0x1a
#define COMMON_CFG_QUEUE_ENABLE 0x1c
#define COMMON_CFG_QUEUE_NOTIFY_OFF 0x1e
#define COMMON_CFG_QUEUE_DESC 0x20
#define COMMON_CFG_QUEUE_DRIVER 0x28
#define COMMON_CFG_QUEUE_DEVICE 0x30

#define QUEUE_INTERRUPT 0x1
#define DEVICE_CONFIG_INTERRUPT 0x2

enum class ConfigurationType : u8 {
    Common = 1,
    Notify = 2,
    ISR = 3,
    Device = 4,
    PCICapabilitiesAccess = 5
};

struct Configuration {
    ConfigurationType cfg_type;
    u8 resource_index; // NOTE: For PCI devices, this is the BAR index
    u32 offset;
    u32 length;
};

}
