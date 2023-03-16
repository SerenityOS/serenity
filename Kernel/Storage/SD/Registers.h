/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

namespace Kernel::SD {

// Relevant Specifications:
// * (SDHC): SD Host Controller Simplified Specification (https://www.sdcard.org/downloads/pls/)
// * (PLSS) Physical Layer Simplified Specification (https://www.sdcard.org/downloads/pls/)
// * (BCM2835) BCM2835 ARM Peripherals (https://www.raspberrypi.org/app/uploads/2012/02/BCM2835-ARM-Peripherals.pdf)

enum class HostVersion : u8 {
    Version1,
    Version2,
    Version3,
    Unknown
};

// SDHC 2.1.1 "SD Host Control Register Map"
// NOTE: The registers must be 32 bits, because of a quirk in the RPI.
struct HostControlRegisterMap {
    u32 argument_2;
    u32 block_size_and_block_count;
    u32 argument_1;
    u32 transfer_mode_and_command;
    u32 response_0;
    u32 response_1;
    u32 response_2;
    u32 response_3;
    u32 buffer_data_port;
    u32 present_state;
    u32 host_configuration_0;
    u32 host_configuration_1;
    u32 interrupt_status;
    u32 interrupt_status_enable;
    u32 interrupt_signal_enable;
    u32 host_configuration_2;
    u32 capabilities_0;
    u32 capabilities_1;
    u32 maximum_current_capabilities;
    u32 maximum_current_capabilities_reserved;
    u32 force_event_for_auto_cmd_error_status;
    u32 adma_error_status;
    u32 adma_system_address[2];
    u32 preset_value[4];
    u32 reserved_0[28];
    u32 shared_bus_control;
    u32 reserved_1[6];
    struct [[gnu::packed]] {
        u8 interrupt_signal_for_each_slot;
        u8 : 8;
        HostVersion specification_version_number;
        u8 vendor_version_number;
    } slot_interrupt_status_and_version;
};
static_assert(AssertSize<HostControlRegisterMap, 256>());

// PLSS 5.1: "OCR Register"
union OperatingConditionRegister {
    u32 raw;
    struct {
        u32 : 15;
        u32 vdd_voltage_window_27_28 : 1;
        u32 vdd_voltage_window_28_29 : 1;
        u32 vdd_voltage_window_29_30 : 1;
        u32 vdd_voltage_window_30_31 : 1;
        u32 vdd_voltage_window_31_32 : 1;
        u32 vdd_voltage_window_32_33 : 1;
        u32 vdd_voltage_window_33_34 : 1;
        u32 vdd_voltage_window_34_35 : 1;
        u32 vdd_voltage_window_35_36 : 1;
        u32 switching_to_18v_accepted : 1;
        u32 : 2;
        u32 over_2tb_support_status : 1;
        u32 : 1;
        u32 uhs2_card_status : 1;
        u32 card_capacity_status : 1;
        u32 card_power_up_status : 1;
    };
};
static_assert(AssertSize<OperatingConditionRegister, 4>());

// PLSS 5.2: "CID Register"
union CardIdentificationRegister {
    u32 raw[4];

    struct [[gnu::packed]] {
        u64 manufacturing_date : 12;
        u64 : 4;
        u64 product_serial_number : 32;
        u64 product_revision : 8;
        u64 product_name : 40;
        u64 oem_id : 16;
        u64 manufacturer_id : 8;
    };
};
static_assert(AssertSize<CardIdentificationRegister, 16>());

// PLSS 5.3.2: "CSD Register (CSD Version 1.0)"
union CardSpecificDataRegister {
    u64 raw[2];

    struct [[gnu::packed]] {
        // Note that the physical layer spec says there are 7 bits of checksum and 1 reserved bit here,
        // but they are removed
        u32 : 1;
        u32 write_protection_until_power_cycle : 1;
        u32 file_format : 2;
        u32 temporary_write_protection : 1;
        u32 permanent_write_protection : 1;
        u32 copy_flag : 1;
        u32 file_format_group : 1;
        u32 : 5;
        u32 partial_blocks_for_write_allowed : 1;
        u32 max_write_data_block_length : 4;
        u32 write_speed_factor : 3;
        u32 : 2;
        u32 write_protect_group_enable : 1;
        u32 write_protect_group_size : 7;
        u32 erase_sector_size : 7;
        u32 erase_single_block_enable : 1;
        u32 device_size_multiplier : 3;
        u32 max_write_current_at_vdd_max : 3;
        u32 max_write_current_at_vdd_min : 3;
        u32 max_read_current_at_vdd_max : 3;
        u32 max_read_current_at_vdd_min : 3;
        u32 device_size : 12;
        u32 : 2;
        u32 dsr_implemented : 1;
        u32 read_block_misalignment : 1;
        u32 write_block_misalignment : 1;
        u32 partial_blocks_for_read_allowed : 1;
        u32 max_read_data_block_length : 4;
        u32 card_command_classes : 12;
        u32 max_data_transfer_rate : 8;
        u32 data_read_access_time2 : 8;
        u32 data_read_access_time1 : 8;
        u32 : 6;
        u32 csd_structure : 2;
    };
};
static_assert(AssertSize<CardSpecificDataRegister, 16>());

// PLSS 5.6: "SCR Register"
union SDConfigurationRegister {
    u8 raw[8];

    struct {
        u32 scr_structure : 4;
        u32 sd_specification : 4;
        u32 data_status_after_erase : 1;
        u32 sd_security : 3;
        u32 sd_bus_widths : 4;
        u32 sd_specification3 : 1;
        u32 extended_security : 4;
        u32 sd_specification4 : 1;
        u32 sd_specification_x : 4;
        u32 : 1;
        u32 command_support : 5;
        u32 : 32;
    };
};
static_assert(AssertSize<SDConfigurationRegister, 8>());

// PLSS 4.10.1: "Card Status"
union CardStatus {
    u32 raw;

    struct {
        u32 : 3;
        u32 ake_seq_error : 1;
        u32 : 1;
        u32 app_cmd : 1;
        u32 fx_event : 1;
        u32 : 1;
        u32 ready_for_data : 1;
        u32 current_state : 4;
        u32 erase_reset : 1;
        u32 card_ecc_disabled : 1;
        u32 wp_erase_skip : 1;
        u32 csd_overwrite : 1;
        u32 : 2;
        u32 error : 1;
        u32 cc_error : 1;
        u32 card_ecc_failed : 1;
        u32 illegal_command : 1;
        u32 com_crc_error : 1;
        u32 lock_unlock_failed : 1;
        u32 card_is_locked : 1;
        u32 wp_violation : 1;
        u32 erase_param : 1;
        u32 erase_seq_error : 1;
        u32 block_len_error : 1;
        u32 address_error : 1;
        u32 out_of_range : 1;
    };
};
static_assert(AssertSize<CardStatus, 4>());

}
