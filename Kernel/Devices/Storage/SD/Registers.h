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

enum class ADMAErrorState : u32 {
    Stop = 0b00,
    FetchDescriptor = 0b01,
    Reserved = 0b10,
    TransferData = 0b11
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
    union PresentState {
        struct { // SDHC 2.2.9 Present State Register (Cat.C Offset 024h)
            u32 command_inhibit_cmd : 1;
            u32 command_inhibit_dat : 1;
            u32 dat_line_active : 1;
            u32 re_tuning_request : 1;
            u32 dat_7_4_line_signal_level : 4;
            u32 write_transfer_active : 1;
            u32 read_transfer_active : 1;
            u32 buffer_write_enable : 1;
            u32 buffer_read_enable : 1;
            u32 : 4;
            u32 card_inserted : 1;
            u32 card_state_stable : 1;
            u32 card_detect_pin_level : 1;
            u32 write_protect_switch_pin_level : 1;
            u32 dat_3_0_line_signal_level : 4;
            u32 cmd_line_signal_level : 1;
            u32 host_regulator_voltage_stable : 1;
            u32 : 1;
            u32 command_not_issued_by_error : 1;
            u32 sub_command_status : 1;
            u32 in_dormant_state : 1;
            u32 lane_synchronization : 1;
            u32 uhs_2_if_detection : 1;
        };
        u32 raw;
    } present_state;
    u32 host_configuration_0;
    u32 host_configuration_1;
    union InterruptStatus {
        struct { // SDHC 2.2.18 Normal Interrupt Status Register (Cat.C Offset 030h)
            u32 command_complete : 1;
            u32 transfer_complete : 1;
            u32 block_gap_event : 1;
            u32 dma_interrupt : 1;
            u32 buffer_write_ready : 1;
            u32 buffer_read_ready : 1;
            u32 card_insertion : 1;
            u32 card_removal : 1;
            u32 card_interrupt : 1;
            u32 int_a : 1;
            u32 int_b : 1;
            u32 int_c : 1;
            u32 retuning_event : 1;
            u32 fx_event : 1;
            u32 : 1;
            u32 error_interrupt : 1;
            // SDHC 2.2.19 Error Interrupt Status Register (Cat.C Offset 032
            u32 command_timeout_error : 1;
            u32 command_crc_error : 1;
            u32 cammand_index_error : 1;
            u32 data_timeout_error : 1;
            u32 data_crc_error : 1;
            u32 data_end_bit_error : 1;
            u32 current_limit_error : 1;
            u32 auto_cmd_error : 1;
            u32 adma_error : 1;
            u32 tuning_error : 1;
            u32 response_error : 1;
            u32 vendor_specific_error : 1;
        } const;
        u32 raw;
    } interrupt_status;
    u32 interrupt_status_enable;
    u32 interrupt_signal_enable;
    u32 host_configuration_2;
    // SDHC 2.2.26 Capabilities Register (Cat.C Offset 040h)
    struct CapabilitesRegister {
        u32 timeout_clock_frequency : 6;
        u32 : 1;
        u32 timeout_clock_unit : 1;
        u32 base_clock_frequency : 8;
        u32 max_block_length : 2;
        u32 eight_bit_support_for_embedded_devices : 1;
        u32 adma2 : 1;
        u32 : 1;
        u32 high_speed : 1;
        u32 sdma : 1;
        u32 suspend_resume : 1;
        u32 three_point_three_volt : 1;
        u32 three_point_zero_volt : 1;
        u32 one_point_eight_volt : 1;
        u32 dma_64_bit_addressing_v4 : 1;
        u32 dma_64_bit_addressing_v3 : 1;
        u32 async_interrupt : 1;
        u32 slot_type : 2;
        u32 sdr50 : 1;
        u32 sdr140 : 1;
        u32 ddr50 : 1;
        u32 uhs_ii : 1;
        u32 driver_type_A : 1;
        u32 driver_type_C : 1;
        u32 driver_type_D : 1;
        u32 : 1;
        u32 timer_count_for_retuning : 4;
        u32 : 1;
        u32 use_tuning_for_sdr50 : 1;
        u32 retuning_modes : 2;
        u32 clock_multiplier : 8;
        u32 : 3;
        u32 adma3 : 1;
        u32 one_point_eight_vdd2 : 1;
        u32 : 3;
    } capabilities;

    u32 maximum_current_capabilities;
    u32 maximum_current_capabilities_reserved;
    u32 force_event_for_auto_cmd_error_status;
    struct {
        ADMAErrorState state : 2;
        u32 length_mismatch_error : 1;
        u32 : 5;
        u32 : 24;
    } adma_error_status;
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

// SDHC Figure 1-10 : General Descriptor Table Format
enum class DMAAction : u8 {
    // ADMA 2
    Nop = 0b000,
    Rsv0 = 0b010,
    Tran = 0b100,
    Link = 0b110,
    // ADMA 3
    CommandDescriptor_SD = 0b001,
    CommandDescriptor_UHS_II = 0b011,
    Rsv1 = 0b101,
    IntegratedDescriptor = 0b111,
};

// Both of these represent the ADMA2 version, ADMA3 might have slight differences
// SDHC 1.13.3.1 ADMA2 Descriptor Format
struct alignas(4) DMADescriptor64 {
    u32 valid : 1;
    u32 end : 1;
    u32 interrupt : 1;
    DMAAction action : 3;
    u32 length_upper : 10; // Version 4.10+ only
    u32 length_lower : 16;
    u32 address : 32;
};
static_assert(AssertSize<DMADescriptor64, 8>());

struct alignas(8) DMADescriptor128 {
    u32 valid : 1;
    u32 end : 1;
    u32 interrupt : 1;
    DMAAction action : 3;
    u32 length_upper : 10; // Version 4.10+ only
    u32 length_lower : 16;
    u32 address_low : 32;
    u32 address_high : 32;
    u32 : 32;
};
static_assert(AssertSize<DMADescriptor128, 16>());

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
