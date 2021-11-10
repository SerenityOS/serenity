/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RecursionDecision.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>

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

class ISO9660Inode;
class ISO9660DirectoryIterator;

class ISO9660FS final : public BlockBasedFileSystem {
    friend ISO9660Inode;
    friend ISO9660DirectoryIterator;

public:
    struct DirectoryEntry : public RefCounted<DirectoryEntry> {
        u32 extent { 0 };
        u32 length { 0 };

        // NOTE: This can never be empty if we read the directory successfully.
        //       We need it as an OwnPtr to default-construct this struct.
        OwnPtr<KBuffer> blocks;

        static ErrorOr<NonnullRefPtr<DirectoryEntry>> try_create(u32 extent, u32 length, OwnPtr<KBuffer> blocks)
        {
            return adopt_nonnull_ref_or_enomem(new (nothrow) DirectoryEntry(extent, length, move(blocks)));
        }

    private:
        DirectoryEntry(u32 extent, u32 length, OwnPtr<KBuffer> blocks)
            : extent(extent)
            , length(length)
            , blocks(move(blocks))
        {
        }
    };

    static ErrorOr<NonnullRefPtr<ISO9660FS>> try_create(OpenFileDescription&);

    virtual ~ISO9660FS() override;
    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "ISO9660FS"sv; }
    virtual Inode& root_inode() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned total_inode_count() const override;

    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    ErrorOr<NonnullRefPtr<DirectoryEntry>> directory_entry_for_record(Badge<ISO9660DirectoryIterator>, ISO::DirectoryRecordHeader const* record);

private:
    ISO9660FS(OpenFileDescription&);

    ErrorOr<void> parse_volume_set();
    ErrorOr<void> create_root_inode();
    ErrorOr<void> calculate_inode_count() const;

    u32 calculate_directory_entry_cache_key(ISO::DirectoryRecordHeader const&);

    ErrorOr<void> visit_directory_record(ISO::DirectoryRecordHeader const& record, Function<ErrorOr<RecursionDecision>(ISO::DirectoryRecordHeader const*)> const& visitor) const;

    OwnPtr<ISO::PrimaryVolumeDescriptor> m_primary_volume;
    RefPtr<ISO9660Inode> m_root_inode;

    mutable u32 m_cached_inode_count { 0 };
    HashMap<u32, NonnullRefPtr<DirectoryEntry>> m_directory_entry_cache;
};

class ISO9660Inode final : public Inode {
    friend ISO9660FS;

public:
    virtual ~ISO9660Inode() override;

    ISO9660FS& fs() { return static_cast<ISO9660FS&>(Inode::fs()); }
    ISO9660FS const& fs() const { return static_cast<ISO9660FS const&>(Inode::fs()); }

    // ^Inode
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> set_atime(time_t) override;
    virtual ErrorOr<void> set_ctime(time_t) override;
    virtual ErrorOr<void> set_mtime(time_t) override;
    virtual void one_ref_left() override;

private:
    // HACK: The base ISO 9660 standard says the maximum filename length is 37
    // bytes large; however, we can read filenames longer than that right now
    // without any problems, so let's allow it anyway.
    static constexpr size_t max_file_identifier_length = 256 - sizeof(ISO::DirectoryRecordHeader);

    ISO9660Inode(ISO9660FS&, ISO::DirectoryRecordHeader const& record, StringView name);
    static ErrorOr<NonnullRefPtr<ISO9660Inode>> try_create_from_directory_record(ISO9660FS&, ISO::DirectoryRecordHeader const& record, StringView name);

    static InodeIndex get_inode_index(ISO::DirectoryRecordHeader const& record, StringView name);
    static StringView get_normalized_filename(ISO::DirectoryRecordHeader const& record, Bytes buffer);

    void create_metadata();
    time_t parse_numerical_date_time(ISO::NumericalDateAndTime const&);

    InodeMetadata m_metadata;
    ISO::DirectoryRecordHeader m_record;
};

}

using Kernel::ISO::has_any_flag;
using Kernel::ISO::has_flag;
