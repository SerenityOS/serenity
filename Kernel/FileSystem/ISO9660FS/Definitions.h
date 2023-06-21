/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

namespace ISO {

// The implemented spec here is ECMA 119, available at:
// https://www.ecma-international.org/wp-content/uploads/ECMA-119_4th_edition_june_2019.pdf

template<typename T>
struct [[gnu::packed]] LittleAndBigEndian {
    T little;
    T big;
};

// 8.4.26.1 Date and Time Format
struct [[gnu::packed]] AsciiDateAndTime {
    // All of these fields are ASCII digits. :^)
    u8 year[4];
    u8 month[2];
    u8 day[2];

    u8 hour[2];
    u8 minute[2];
    u8 second[2];
    u8 hundredths_of_second[2];

    // From OSDev wiki:
    // Time zone offset from GMT in 15 minute intervals, starting at
    // interval -48 (west) and running up to interval 52 (east). So value 0
    // indicates interval -48 which equals GMT-12 hours, and value 100
    // indicates interval 52 which equals GMT+13 hours.
    u8 timezone_offset;
};
static_assert(sizeof(AsciiDateAndTime) == 17);

// 9.1.5 Recording Date and Time (BP 19 to 25)
struct [[gnu::packed]] NumericalDateAndTime {
    u8 years_since_1900;
    u8 month;
    u8 day;
    u8 hour;
    u8 minute;
    u8 second;
    // Same format as AsciiDateAndTime.
    u8 timezone_offset;
};
static_assert(sizeof(NumericalDateAndTime) == 7);

// --- Path Table ---

// 9.4 Format of a Path Table Record
struct [[gnu::packed]] PathTableRecord {
    u8 directory_identifier_length;
    u8 extended_attribute_record_length;
    u32 extent_location;
    u16 parent_directory_number;

    u8 directory_identifier[];
};
static_assert(sizeof(PathTableRecord) == 8);

// --- Extended Attribute Record ---

// 9.5.3 Permissions
enum class ExtendedPermissions : u16 {
    SystemGroupReadable = 1 << 0,
    SystemGroupExecutable = 1 << 2,
    UserReadable = 1 << 4,
    UserExecutable = 1 << 6,
    GroupReadable = 1 << 8,
    GroupExecutable = 1 << 10,
    OtherReadable = 1 << 12,
    OtherExecutable = 1 << 14,
};
AK_ENUM_BITWISE_OPERATORS(ExtendedPermissions);

// 9.5.8 Record Format
enum class RecordFormat : u8 {
    NotSpecified = 0,
    FixedLengthRecords = 1,
    LittleEndianVariableRecords = 2,
    BigEndianVariableRecords = 3,
    // 4-127 are reserved for future standardization.
    // 128-255 are reserved for system use.
};

// 9.5.9 Record Attributes
enum class RecordAttributes : u8 {
    // This value means the record is stored like: \n123456\r.
    LfCrDelimited = 0,
    FortranVerticalSpacing = 1,
    ContainsControlInformation = 2,
    // 3-255 are reserved for future standardization.
};

// 9.5 Format of an Extended Attribute Record
struct [[gnu::packed]] ExtendedAttributeRecord {
    LittleAndBigEndian<u16> owner_identification;
    LittleAndBigEndian<u16> group_identification;
    ExtendedPermissions permissions;

    AsciiDateAndTime file_creation_date_and_time;
    AsciiDateAndTime file_modification_date_and_time;
    AsciiDateAndTime file_expiration_date_and_time;
    AsciiDateAndTime file_effective_date_and_time;

    RecordFormat record_format;
    u8 record_attributes;

    LittleAndBigEndian<u16> record_length;

    u8 system_identifier[32];
    u8 system_use[64];

    u8 extended_attribute_record_version;
    u8 escape_sequence_length;

    u8 reserved[64];

    LittleAndBigEndian<u16> application_use_length;

    // NOTE: Application use is immediately followed by escape sequences (no
    // padding).
    u8 application_use_and_escape_sequences[];
};
static_assert(sizeof(ExtendedAttributeRecord) == 250);

// --- Files and Directories ---

// 9.1.6 File Flags
enum class FileFlags : u8 {
    Hidden = 1 << 0, // The "existence" flag
    Directory = 1 << 1,
    AssociatedFile = 1 << 2,
    Record = 1 << 3,
    Protection = 1 << 4,
    // 5 and 6 are reserved.
    MultiExtent = 1 << 7,
};

AK_ENUM_BITWISE_OPERATORS(FileFlags);

struct [[gnu::packed]] DirectoryRecordHeader {
    u8 length;
    u8 extended_attribute_record_length;
    LittleAndBigEndian<u32> extent_location;
    LittleAndBigEndian<u32> data_length;
    NumericalDateAndTime recording_date_and_time;
    FileFlags file_flags;
    u8 file_unit_size;
    u8 interleave_gap_size;
    LittleAndBigEndian<u16> volume_sequence_number;
    u8 file_identifier_length;

    // NOTE: The file identifier itself is of variable length, so it and the
    // fields following it are not included in this struct. Instead, they are:
    //
    // 34 to (33+file_identifier_length) - file identifier
    // 1 byte of padding, if file_identifier_length is even
    //
    // The remaining bytes are system use (ISO9660 extensions).
};
static_assert(sizeof(DirectoryRecordHeader) == 33);

// --- Volume Descriptors ---

enum class VolumeDescriptorType : u8 {
    BootRecord = 0,
    PrimaryVolumeDescriptor = 1,
    SupplementaryOrEnhancedVolumeDescriptor = 2,
    VolumePartitionDescriptor = 3,
    // 4-254 are reserved.
    VolumeDescriptorSetTerminator = 255,
};

// 8.1 Format of a Volume Descriptor
struct [[gnu::packed]] VolumeDescriptorHeader {
    VolumeDescriptorType type;
    // NOTE: Contains exactly "CD001".
    u8 identifier[5];
    u8 version;
};
static_assert(sizeof(VolumeDescriptorHeader) == 7);

// 8.2 Boot Record
struct [[gnu::packed]] BootRecord {
    VolumeDescriptorHeader header;
    u8 boot_system_identifier[32];
    u8 boot_identifier[32];
    u8 boot_system_use[1977];
};
static_assert(sizeof(BootRecord) == 2048);

// 8.3 Volume Descriptor Set Terminator
struct [[gnu::packed]] VolumeDescriptorSetTerminator {
    VolumeDescriptorHeader header;
    u8 zeros[2041];
};
static_assert(sizeof(VolumeDescriptorSetTerminator) == 2048);

// 8.4 Primary Volume Descriptor
struct [[gnu::packed]] PrimaryVolumeDescriptor {
    VolumeDescriptorHeader header;
    u8 unused1;
    u8 system_identifier[32];
    u8 volume_identifier[32];
    u64 unused2;
    LittleAndBigEndian<u32> volume_space_size;
    u8 unused3[32];
    LittleAndBigEndian<u16> volume_set_size;
    LittleAndBigEndian<u16> volume_sequence_number;
    LittleAndBigEndian<u16> logical_block_size;
    LittleAndBigEndian<u32> path_table_size;

    u32 l_path_table_occurrence_location;
    u32 l_path_table_optional_occurrence_location;
    u32 m_path_table_occurrence_location;
    u32 m_path_table_optional_occurrence_location;

    DirectoryRecordHeader root_directory_record_header;
    u8 root_directory_identifier; // Exactly 0x00.

    u8 volume_set_identifier[128];
    u8 publisher_identifier[128];
    u8 data_preparer_identifier[128];
    u8 application_identifier[128];

    u8 copyright_file_identifier[37];
    u8 abstract_file_identifier[37];
    u8 bibliographic_file_identifier[37];

    AsciiDateAndTime volume_creation_date_and_time;
    AsciiDateAndTime volume_modification_date_and_time;
    AsciiDateAndTime volume_expiration_date_and_time;
    AsciiDateAndTime volume_effective_date_and_time;

    u8 file_structure_version; // Always 0x01.
    u8 unused4;
    u8 application_use[512];
    u8 reserved[653];
};
static_assert(sizeof(PrimaryVolumeDescriptor) == 2048);

// 8.6 Volume Partition Descriptor
struct [[gnu::packed]] VolumePartitionDescriptor {
    VolumeDescriptorHeader header;
    u8 unused;

    u8 system_identifier[32];
    u8 volume_partition_identifier[32];
    LittleAndBigEndian<u32> volume_partition_location;
    LittleAndBigEndian<u32> volume_partition_size;

    u8 system_use[1960];
};
static_assert(sizeof(VolumePartitionDescriptor) == 2048);

}

}
