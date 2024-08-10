/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

namespace Kernel::SCSI {

// 5.4.1
// Table 437 Vital product data page codes
enum class VitalProductDataPageCode : u8 {
    SupportedVitalProductDataPages = 0x00,
    ASCIIInformation = 0x01,
    // 0x02-0x7F Are also ASCII INFORMATION Pages
    UnitSerialNumber = 0x80,
    DeviceIdentification = 0x83,
    SoftwareInterfaceIdentification = 0x84,
    ManagementNetworkAddresses = 0x85,
    ExtendedInquiryData = 0x86,
    ModePagePolicy = 0x87,
    SCSIPorts = 0x88,
    PowerCondition = 0x8A,
    DeviceConstituents = 0x8B,
    CFAProfileInformation = 0x8C,
    PowerConsumption = 0x8D,
    BlockLimits = 0xB0,
    BlockDeviceCharacteristics = 0xB1,
    LogicalBlockProvisioning = 0xB2,
    Referrals = 0xB3,
    SupportedBlockLengthsAndProtectionTypes = 0xB4,
    BlockDeviceCharacteristicsExtension = 0xB5,
    ZonedBlockDeviceCharacteristics = 0xB6,
    BlockLimitsExtension = 0xB7,
    FirmwareNumbersPage = 0xC0,
    DateCodePage = 0xC1,
    JumperSettingsPage = 0xC2,
    DeviceBehaviorPage = 0xC3,
};

struct VitalProductPage {
    struct {
        u8 device_type : 5;
        u8 qualifier : 3;
    } peripheral_info;
    VitalProductDataPageCode page_code;
    BigEndian<u16> page_length; // N - 3
};
static_assert(AssertSize<VitalProductPage, 0x04>());

// 5.4.5 Block limits page
struct BlockLimitsPage : VitalProductPage {
    u8 write_same_non_zero; // WSNZ 1 bit
    u8 maximum_compare_and_write_length;
    BigEndian<u16> optimal_transfer_length_granularity;
    BigEndian<u32> maximum_transfer_length;
    BigEndian<u32> optimal_transfer_length;
    BigEndian<u32> maximum_prefetch;
    BigEndian<u32> maximum_unmap_lba_count;
    BigEndian<u32> maximum_unmap_block_descriptor_count;
    BigEndian<u32> optimal_unmap_granularity;
    BigEndian<u32> unmap_granularity_alignment; // Also unamap granularity alignment (UGA) valid in the highest bit
    BigEndian<u64> maximum_write_same_length;
    BigEndian<u32> maximum_atomic_transfer_length;
    BigEndian<u32> atomic_alignment;
    BigEndian<u32> atomic_transfer_length_granularity;
    BigEndian<u32> maximum_atomic_transfer_length_with_atomic_boundary;
    BigEndian<u32> maximum_atomic_boundary_size;
};
static_assert(AssertSize<BlockLimitsPage, 0x003C + 4>());

// 5.4.18
struct SupportedVitalProductPages : VitalProductPage {
    // Note: The page length is only 8 bytes for this page
    VitalProductDataPageCode supported_pages[];
};

}
