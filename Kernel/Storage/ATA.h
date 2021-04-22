/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define ATA_SR_BSY 0x80
#define ATA_SR_DRDY 0x40
#define ATA_SR_DF 0x20
#define ATA_SR_DSC 0x10
#define ATA_SR_DRQ 0x08
#define ATA_SR_CORR 0x04
#define ATA_SR_IDX 0x02
#define ATA_SR_ERR 0x01

#define ATA_ER_BBK 0x80
#define ATA_ER_UNC 0x40
#define ATA_ER_MC 0x20
#define ATA_ER_IDNF 0x10
#define ATA_ER_MCR 0x08
#define ATA_ER_ABRT 0x04
#define ATA_ER_TK0NF 0x02
#define ATA_ER_AMNF 0x01

#define ATA_CMD_READ_PIO 0x20
#define ATA_CMD_READ_PIO_EXT 0x24
#define ATA_CMD_READ_DMA 0xC8
#define ATA_CMD_READ_DMA_EXT 0x25
#define ATA_CMD_WRITE_PIO 0x30
#define ATA_CMD_WRITE_PIO_EXT 0x34
#define ATA_CMD_WRITE_DMA 0xCA
#define ATA_CMD_WRITE_DMA_EXT 0x35
#define ATA_CMD_CACHE_FLUSH 0xE7
#define ATA_CMD_CACHE_FLUSH_EXT 0xEA
#define ATA_CMD_PACKET 0xA0
#define ATA_CMD_IDENTIFY_PACKET 0xA1
#define ATA_CMD_IDENTIFY 0xEC

#define ATAPI_CMD_READ 0xA8
#define ATAPI_CMD_EJECT 0x1B

#define ATA_IDENT_DEVICETYPE 0
#define ATA_IDENT_CYLINDERS 2
#define ATA_IDENT_HEADS 6
#define ATA_IDENT_SECTORS 12
#define ATA_IDENT_SERIAL 20
#define ATA_IDENT_MODEL 54
#define ATA_IDENT_CAPABILITIES 98
#define ATA_IDENT_FIELDVALID 106
#define ATA_IDENT_MAX_LBA 120
#define ATA_IDENT_COMMANDSETS 164
#define ATA_IDENT_MAX_LBA_EXT 200

#define ATA_USE_LBA_ADDRESSING (1 << 6)

#define IDE_ATA 0x00
#define IDE_ATAPI 0x01

#define ATA_REG_DATA 0x00
#define ATA_REG_ERROR 0x01
#define ATA_REG_FEATURES 0x01
#define ATA_REG_SECCOUNT0 0x02
#define ATA_REG_LBA0 0x03
#define ATA_REG_LBA1 0x04
#define ATA_REG_LBA2 0x05
#define ATA_REG_HDDEVSEL 0x06
#define ATA_REG_COMMAND 0x07
#define ATA_REG_STATUS 0x07
#define ATA_REG_SECCOUNT1 0x08
#define ATA_REG_LBA3 0x09
#define ATA_REG_LBA4 0x0A
#define ATA_REG_LBA5 0x0B
#define ATA_CTL_CONTROL 0x00
#define ATA_CTL_ALTSTATUS 0x00
#define ATA_CTL_DEVADDRESS 0x01

#define ATA_CAP_LBA 0x200

#include <AK/Types.h>

namespace Kernel {
struct [[gnu::packed]] ATAIdentifyBlock {
    u16 general_configuration;
    u16 obsolete;
    u16 specific_configuration;

    u16 obsolete2;
    u16 retired[2];
    u16 obsolete3;

    u16 reserved_for_cfa[2];
    u16 retired2;
    u16 serial_number[10];

    u16 retired3[2];
    u16 obsolete4;

    u16 firmware_revision[4];
    u16 model_number[20];

    u16 maximum_logical_sectors_per_drq;
    u16 trusted_computing_features;
    u16 capabilities[2];
    u16 obsolete5[2];
    u16 validity_flags;
    u16 obsolete6[5];

    u16 security_features;

    u32 max_28_bit_addressable_logical_sector;
    u16 obsolete7;
    u16 dma_modes;
    u16 pio_modes;

    u16 minimum_multiword_dma_transfer_cycle;
    u16 recommended_multiword_dma_transfer_cycle;

    u16 minimum_multiword_pio_transfer_cycle_without_flow_control;
    u16 minimum_multiword_pio_transfer_cycle_with_flow_control;

    u16 additional_supported;
    u16 reserved3[5];
    u16 queue_depth;

    u16 serial_ata_capabilities;
    u16 serial_ata_additional_capabilities;
    u16 serial_ata_features_supported;
    u16 serial_ata_features_enabled;
    u16 major_version_number;
    u16 minor_version_number;
    u16 commands_and_feature_sets_supported[3];
    u16 commands_and_feature_sets_supported_or_enabled[3];
    u16 ultra_dma_modes;

    u16 timing_for_security_features[2];
    u16 apm_level;
    u16 master_password_id;

    u16 hardware_reset_results;
    u16 obsolete8;

    u16 stream_minimum_request_time;
    u16 streaming_transfer_time_for_dma;
    u16 streaming_access_latency;
    u16 streaming_performance_granularity[2];

    u64 user_addressable_logical_sectors_count;

    u16 streaming_transfer_time_for_pio;
    u16 max_512_byte_blocks_per_data_set_management_command;
    u16 physical_sector_size_to_logical_sector_size;
    u16 inter_seek_delay_for_acoustic_testing;
    u16 world_wide_name[4];
    u16 reserved4[4];
    u16 obsolete9;

    u32 logical_sector_size;

    u16 commands_and_feature_sets_supported2;
    u16 commands_and_feature_sets_supported_or_enabled2;

    u16 reserved_for_expanded_supported_and_enabled_settings[6];
    u16 obsolete10;

    u16 security_status;
    u16 vendor_specific[31];
    u16 reserved_for_cfa2[8];
    u16 device_nominal_form_factor;
    u16 data_set_management_command_support;
    u16 additional_product_id[4];
    u16 reserved5[2];
    u16 current_media_serial_number[30];
    u16 sct_command_transport;
    u16 reserved6[2];

    u16 logical_sectors_alignment_within_physical_sector;

    u32 write_read_verify_sector_mode_3_count;
    u32 write_read_verify_sector_mode_2_count;

    u16 obsolete11[3];
    u16 nominal_media_rotation_rate;
    u16 reserved7;
    u16 obsolete12;
    u16 write_read_verify_feature_set_current_mode;
    u16 reserved8;
    u16 transport_major_version_number;
    u16 transport_minor_version_number;
    u16 reserved9[6];

    u64 extended_user_addressable_logical_sectors_count;

    u16 minimum_512_byte_data_blocks_per_download_microcode_operation;
    u16 max_512_byte_data_blocks_per_download_microcode_operation;

    u16 reserved10[19];
    u16 integrity;
};
};
