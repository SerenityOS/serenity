/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/Types.h>
#include <Kernel/API/FileSystem/FATStructures.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <sys/time.h>
#include <unistd.h>

// Public domain boot code adapted from:
// https://github.com/dosfstools/dosfstools/blob/289a48b9cb5b3c589391d28aa2515c325c932c7a/src/mkfs.fat.c#L205

static u8 s_bootcode[74] = {
    0x0E,             // push cs
    0x1F,             // pop ds
    0xBE, 0x5B, 0x7C, // mov si, offset message_txt
    // write_msg:
    0xAC,             // lodsb
    0x22, 0xC0,       // and al, al
    0x74, 0x0B,       // jz key_press
    0x56,             // push si
    0xB4, 0x0E,       // mov ah, 0eh
    0xBB, 0x07, 0x00, // mov bx, 0007h
    0xCD, 0x10,       // int 10h
    0x5E,             // pop si
    0xEB, 0xF0,       // jmp write_msg
    // key_press:
    0x32, 0xE4, // xor ah, ah
    0xCD, 0x16, // int 16h
    0xCD, 0x19, // int 19h
    0xEB, 0xFE, // foo: jmp foo
    // message_txt:
    '\r', '\n',
    'N', 'o', 'n', '-', 's', 'y', 's', 't',
    'e', 'm', ' ', 'd', 'i', 's', 'k',
    '\r', '\n',
    'P', 'r', 'e', 's', 's', ' ', 'a', 'n',
    'y', ' ', 'k', 'e', 'y', ' ', 't', 'o',
    ' ', 'r', 'e', 'b', 'o', 'o', 't',
    '\r', '\n',
    0
};

// FIXME: Modify the boot code to use relative offsets.
static constexpr size_t s_message_offset_offset = 3;

struct DiskSizeToSectorsPerClusterMapping {
    u32 disk_size;
    u8 sectors_per_cluster;
};

// NOTE: Unlike when using the tables after this one, the values here should only
// be used if the given disk size is an exact match.
static constexpr auto s_disk_table_fat12 = to_array<DiskSizeToSectorsPerClusterMapping>({
    { 720, 2 },  // 360K floppies
    { 1440, 2 }, // 720K floppies
    { 2400, 1 }, // 1200K floppies
    { 2880, 1 }, // 1440K floppies
    { 5760, 2 }, // 2880K floppies
});

static constexpr auto s_disk_table_fat16 = to_array<DiskSizeToSectorsPerClusterMapping>({
    { 8400, 0 },       // disks up to 4.1 MiB, the 0 value for trips an error
    { 32680, 2 },      // disks up to 16 MiB, 1k cluster
    { 262144, 4 },     // disks up to 128 MiB, 2k cluster
    { 524288, 8 },     // disks up to 256 MiB, 4k cluster
    { 1048576, 16 },   // disks up to 512 MiB, 8k cluster
    { 2097152, 32 },   // disks up to 1 GiB, 16k cluster
    { 4194304, 64 },   // disks up to 2 GiB, 32k cluster
    { 0xFFFFFFFF, 0 }, // any disk greater than 2GiB, the 0 value trips an error
});

static constexpr auto s_disk_table_fat32 = to_array<DiskSizeToSectorsPerClusterMapping>({
    { 66600, 0 },       // disks up to 32.5 MiB, the 0 value trips an error
    { 532480, 1 },      // disks up to 260 MiB, .5k cluster
    { 16777216, 8 },    // disks up to 8 GiB, 4k cluster
    { 33554432, 16 },   // disks up to 16 GiB, 8k cluster
    { 67108864, 32 },   // disks up to 32 GiB, 16k cluster
    { 0xFFFFFFFF, 64 }, // disks greater than 32GiB, 32k cluster
});

static constexpr u8 s_boot_signature[2] = { 0x55, 0xAA };
static constexpr u8 s_empty_12_bit_fat[4] = { 0xF0, 0xFF, 0xFF, 0x00 };
static constexpr u8 s_empty_16_bit_fat[4] = { 0xF8, 0xFF, 0xFF, 0xFF };
static constexpr u8 s_empty_32_bit_fat[12] = { 0xF8, 0xFF, 0xFF, 0x0F, 0xFF, 0xFF, 0xFF, 0x0F, 0xFF, 0xFF, 0xFF, 0x0F };

static constexpr u8 s_zero_buffer[4096] { 0 };

static constexpr u8 s_fat_entry_size { 32 };

enum class FATType {
    FAT12,
    FAT16,
    FAT32,
};

template<typename T>
static void write_little_endian(T value, u8* output, size_t& offset)
{
    value = AK::convert_between_host_and_little_endian(value);
    __builtin_memcpy(output + offset, reinterpret_cast<u8 const*>(&value), sizeof(T));
    offset += sizeof(T);
}

static void write_to_buffer(u8* output, ReadonlyBytes source, size_t& offset)
{
    __builtin_memcpy(output + offset, source.data(), source.size());
    offset += source.size();
}

static Array<u8, sizeof(Kernel::DOS3BIOSParameterBlock)> serialize_dos_3_bios_parameter_block(Kernel::DOS3BIOSParameterBlock const& boot_record)
{
    Array<u8, sizeof(Kernel::DOS3BIOSParameterBlock)> output;
    size_t offset = 0;

    write_to_buffer(output.data(), { &boot_record.boot_jump, sizeof(boot_record.boot_jump) }, offset);
    write_to_buffer(output.data(), { &boot_record.oem_identifier, sizeof(boot_record.oem_identifier) }, offset);
    write_little_endian(boot_record.bytes_per_sector, output.data(), offset);
    write_little_endian(boot_record.sectors_per_cluster, output.data(), offset);
    write_little_endian(boot_record.reserved_sector_count, output.data(), offset);
    write_little_endian(boot_record.fat_count, output.data(), offset);
    write_little_endian(boot_record.root_directory_entry_count, output.data(), offset);
    write_little_endian(boot_record.sector_count_16bit, output.data(), offset);
    write_little_endian(boot_record.media_descriptor_type, output.data(), offset);
    write_little_endian(boot_record.sectors_per_fat_16bit, output.data(), offset);
    write_little_endian(boot_record.sectors_per_track, output.data(), offset);
    write_little_endian(boot_record.head_count, output.data(), offset);
    write_little_endian(boot_record.hidden_sector_count, output.data(), offset);
    write_little_endian(boot_record.sector_count_32bit, output.data(), offset);

    return output;
}

static Array<u8, sizeof(Kernel::DOS4BIOSParameterBlock)> serialize_dos_4_bios_parameter_block(Kernel::DOS4BIOSParameterBlock const& boot_record)
{
    Array<u8, sizeof(Kernel::DOS4BIOSParameterBlock)> output;
    size_t offset = 0;

    write_little_endian(boot_record.drive_number, output.data(), offset);
    write_little_endian(boot_record.flags, output.data(), offset);
    write_little_endian(boot_record.signature, output.data(), offset);
    write_little_endian(boot_record.volume_id, output.data(), offset);
    write_to_buffer(output.data(), { &boot_record.volume_label_string, sizeof(boot_record.volume_label_string) }, offset);
    write_to_buffer(output.data(), { &boot_record.file_system_type, sizeof(boot_record.file_system_type) }, offset);

    return output;
}

static Array<u8, sizeof(Kernel::DOS7BIOSParameterBlock)> serialize_dos_7_bios_parameter_block(Kernel::DOS7BIOSParameterBlock const& boot_record)
{
    Array<u8, sizeof(Kernel::DOS7BIOSParameterBlock)> output;
    size_t offset = 0;

    write_little_endian(boot_record.sectors_per_fat_32bit, output.data(), offset);
    write_little_endian(boot_record.flags, output.data(), offset);
    write_little_endian(boot_record.fat_version, output.data(), offset);
    write_little_endian(boot_record.root_directory_cluster, output.data(), offset);
    write_little_endian(boot_record.fs_info_sector, output.data(), offset);
    write_little_endian(boot_record.backup_boot_sector, output.data(), offset);
    write_to_buffer(output.data(), { &boot_record.unused3, sizeof(boot_record.unused3) }, offset);
    write_little_endian(boot_record.drive_number, output.data(), offset);
    write_little_endian(boot_record.unused4, output.data(), offset);
    write_little_endian(boot_record.signature, output.data(), offset);
    write_little_endian(boot_record.volume_id, output.data(), offset);
    write_to_buffer(output.data(), { &boot_record.volume_label_string, sizeof(boot_record.volume_label_string) }, offset);
    write_to_buffer(output.data(), { &boot_record.file_system_type, sizeof(boot_record.file_system_type) }, offset);

    return output;
}

static Array<u8, sizeof(Kernel::FAT32FSInfo)> serialize_fat32_fs_info(Kernel::FAT32FSInfo const& fs_info)
{
    Array<u8, sizeof(Kernel::FAT32FSInfo)> output;
    size_t offset = 0;

    write_little_endian(fs_info.lead_signature, output.data(), offset);
    write_to_buffer(output.data(), { &fs_info.unused1, sizeof(fs_info.unused1) }, offset);
    write_little_endian(fs_info.struct_signature, output.data(), offset);
    write_little_endian(fs_info.last_known_free_cluster_count, output.data(), offset);
    write_little_endian(fs_info.next_free_cluster_hint, output.data(), offset);
    write_to_buffer(output.data(), { &fs_info.unused2, sizeof(fs_info.unused2) }, offset);
    write_little_endian(fs_info.trailing_signature, output.data(), offset);

    return output;
}

// This algorithm only works for 512 byte sectors, which are the only ones we support anyway.
// This may also produce slightly inefficient results, using up to 2 extra sectors for FAT16 and up to 8 for FAT32.
static u32 get_sectors_per_fat(Kernel::DOS3BIOSParameterBlock const& boot_record, FATType fat_type)
{
    if (fat_type == FATType::FAT12) {
        switch (boot_record.sector_count_16bit / 2) {
        case 360:
            return 3;
        case 720:
            return 5;
        case 1200:
            return 7;
        case 1440:
        case 2880:
            return 9;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    u32 sector_count = boot_record.sector_count_32bit;

    if (fat_type == FATType::FAT16 && boot_record.sector_count_16bit != 0)
        sector_count = boot_record.sector_count_16bit;

    VERIFY(sector_count != 0);

    u32 root_directory_sectors = ((boot_record.root_directory_entry_count * s_fat_entry_size) + (boot_record.bytes_per_sector - 1)) / boot_record.bytes_per_sector;
    u32 sectors_per_container = sector_count - (boot_record.reserved_sector_count + root_directory_sectors);
    u32 container_count = (256 * boot_record.sectors_per_cluster) + boot_record.fat_count;

    if (fat_type == FATType::FAT32)
        container_count /= 2;

    return (sectors_per_container + (container_count - 1)) / container_count;
}

static ErrorOr<Kernel::DOS3BIOSParameterBlock> generate_dos_3_bios_parameter_block(u64 file_size, FATType fat_type)
{
    Kernel::DOS3BIOSParameterBlock boot_record {};
    boot_record.boot_jump[0] = 0xEB; // jmp
    switch (fat_type) {
    case FATType::FAT12:
    case FATType::FAT16:
        boot_record.boot_jump[1] = sizeof(Kernel::DOS3BIOSParameterBlock) + sizeof(Kernel::DOS4BIOSParameterBlock) - 2;
        break;
    case FATType::FAT32:
        boot_record.boot_jump[1] = sizeof(Kernel::DOS3BIOSParameterBlock) + sizeof(Kernel::DOS7BIOSParameterBlock) - 2;
        break;
    }
    boot_record.boot_jump[2] = 0x90; // nop

    __builtin_memcpy(&boot_record.oem_identifier[0], "MSWIN4.1", 8);
    boot_record.bytes_per_sector = 512;
    boot_record.sectors_per_cluster = 0;
    // These are set early since we'll need one of them to look up the amount of sectors per cluster.
    switch (fat_type) {
    case FATType::FAT12:
    case FATType::FAT16:
        if (file_size / boot_record.bytes_per_sector <= NumericLimits<u16>::max()) {
            boot_record.sector_count_16bit = file_size / boot_record.bytes_per_sector;
            boot_record.sector_count_32bit = 0;
            break;
        }
        [[fallthrough]];
    case FATType::FAT32:
        boot_record.sector_count_16bit = 0;
        boot_record.sector_count_32bit = file_size / boot_record.bytes_per_sector;
        break;
    }

    // FAT12 and FAT16 may place the total sector count in either sector_count_16bit or sector_count_32bit,
    // depending on where it fits. Currently we only support FAT12 on floppy disks,
    // where the sector count will always fit in sector_count_16bit, so this should only be used when dealing with FAT16.
    auto get_fat16_sector_count = [&]() -> u32 {
        VERIFY(fat_type == FATType::FAT16);
        if (boot_record.sector_count_16bit != 0) {
            return boot_record.sector_count_16bit;
        }

        return boot_record.sector_count_32bit;
    };

    switch (fat_type) {
    case FATType::FAT12:
        for (auto const& potential_entry : s_disk_table_fat12) {
            if (boot_record.sector_count_16bit == potential_entry.disk_size) {
                boot_record.sectors_per_cluster = potential_entry.sectors_per_cluster;
                break;
            }
        }

        if (boot_record.sectors_per_cluster == 0)
            return Error::from_string_literal("Unsupported partition size for FAT12 (supported sizes are 360K, 720K, 1200K, 1440K and 2880K)");

        break;
    case FATType::FAT16:
        for (auto const& potential_entry : s_disk_table_fat16) {
            if (get_fat16_sector_count() <= potential_entry.disk_size) {
                boot_record.sectors_per_cluster = potential_entry.sectors_per_cluster;
                break;
            }
        }

        if (boot_record.sectors_per_cluster == 0) {
            if (get_fat16_sector_count() <= s_disk_table_fat16[0].disk_size) {
                return Error::from_string_literal("Partition too small for FAT16");
            }

            return Error::from_string_literal("Partition too large for FAT16");
        }
        break;
    case FATType::FAT32:
        for (auto const& potential_entry : s_disk_table_fat32) {
            if (boot_record.sector_count_32bit <= potential_entry.disk_size) {
                boot_record.sectors_per_cluster = potential_entry.sectors_per_cluster;
                break;
            }
        }

        if (boot_record.sectors_per_cluster == 0)
            return Error::from_string_literal("Partition too small for FAT32");

        break;
    }

    VERIFY(boot_record.bytes_per_sector * boot_record.sectors_per_cluster <= 32 * KiB);

    boot_record.fat_count = 2;

    switch (fat_type) {
    case FATType::FAT12:
        boot_record.reserved_sector_count = 1;
        switch (file_size / 1024) {
        case 360:
        case 720:
            boot_record.root_directory_entry_count = 112;
            break;
        case 1200:
        case 1440:
        case 2880:
            boot_record.root_directory_entry_count = 224;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case FATType::FAT16:
        boot_record.reserved_sector_count = 1;
        boot_record.root_directory_entry_count = 512;
        break;
    case FATType::FAT32:
        boot_record.reserved_sector_count = 32;
        boot_record.root_directory_entry_count = 0;
        break;
    }

    switch (fat_type) {
    case FATType::FAT12:
        boot_record.head_count = 2;
        switch (file_size / 1024) {
        case 360:
            boot_record.media_descriptor_type = 0xFD;
            boot_record.sectors_per_track = 9;
            break;
        case 720:
            boot_record.media_descriptor_type = 0xF9;
            boot_record.sectors_per_track = 9;
            break;
        case 1200:
            boot_record.media_descriptor_type = 0xF9;
            boot_record.sectors_per_track = 15;
            break;
        case 1440:
            boot_record.media_descriptor_type = 0xF0;
            boot_record.sectors_per_track = 18;
            break;
        case 2880:
            boot_record.media_descriptor_type = 0xF0;
            boot_record.sectors_per_track = 36;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case FATType::FAT16:
    case FATType::FAT32:
        // FIXME: Fill in real values for these when dealing with hardware where disk geometry is relevant.
        boot_record.media_descriptor_type = 0xF8; // This is a fixed disk, i.e. a partition on a hard drive.
        boot_record.sectors_per_track = 63;
        boot_record.head_count = 255;
    }

    boot_record.hidden_sector_count = 0;

    // Fill in sectors_per_fat_16bit in last to make sure we've set everything that get_sectors_per_fat needs.
    switch (fat_type) {
    case FATType::FAT12:
    case FATType::FAT16:
        boot_record.sectors_per_fat_16bit = get_sectors_per_fat(boot_record, fat_type);
        break;
    case FATType::FAT32:
        boot_record.sectors_per_fat_16bit = 0;
        break;
    }

    return boot_record;
}

static Kernel::DOS4BIOSParameterBlock generate_dos_4_bios_parameter_block(FATType fat_type, u32 volume_id)
{
    Kernel::DOS4BIOSParameterBlock boot_record_16_bit {};
    if (fat_type == FATType::FAT12)
        boot_record_16_bit.drive_number = 0x00; // Signify that this is a floppy disk.
    else
        boot_record_16_bit.drive_number = 0x80; // Signify that this is a hard disk.
    boot_record_16_bit.flags = 0;
    boot_record_16_bit.signature = 0x29;
    boot_record_16_bit.volume_id = volume_id;
    __builtin_memcpy(&boot_record_16_bit.volume_label_string[0], "NO NAME    ", 11); // Must be padded with spaces.
    switch (fat_type) {
    case FATType::FAT12:
        __builtin_memcpy(&boot_record_16_bit.file_system_type[0], "FAT12   ", 8);
        break;
    case FATType::FAT16:
        __builtin_memcpy(&boot_record_16_bit.file_system_type[0], "FAT16   ", 8);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return boot_record_16_bit;
}

static Kernel::DOS7BIOSParameterBlock generate_dos_7_bios_parameter_block(Kernel::DOS3BIOSParameterBlock const& boot_record, u32 volume_id)
{
    Kernel::DOS7BIOSParameterBlock boot_record_fat32 {};
    boot_record_fat32.sectors_per_fat_32bit = get_sectors_per_fat(boot_record, FATType::FAT32);
    boot_record_fat32.flags = 0;
    boot_record_fat32.fat_version = 0;
    boot_record_fat32.root_directory_cluster = 2;
    boot_record_fat32.fs_info_sector = 1;
    boot_record_fat32.backup_boot_sector = 6;

    __builtin_memset(&boot_record_fat32.unused3[0], 0, 12); // Reserved field.

    boot_record_fat32.drive_number = 0x80; // Signify that this is a hard disk.
    boot_record_fat32.unused4 = 0;         // Windows NT flags.
    boot_record_fat32.signature = 0x29;

    boot_record_fat32.volume_id = volume_id;

    __builtin_memcpy(&boot_record_fat32.volume_label_string[0], "NO NAME    ", 11); // Must be padded with spaces.
    __builtin_memcpy(&boot_record_fat32.file_system_type[0], "FAT32   ", 8);

    return boot_record_fat32;
}

static Kernel::FAT32FSInfo generate_fat32_fs_info(Kernel::DOS3BIOSParameterBlock const& boot_record, Kernel::DOS7BIOSParameterBlock const& boot_record_fat32)
{
    Kernel::FAT32FSInfo fs_info {};
    fs_info.lead_signature = 0x41615252;
    __builtin_memset(&fs_info.unused1[0], 0, 480);
    fs_info.struct_signature = 0x61417272;
    {
        off_t const fat_sectors = boot_record_fat32.sectors_per_fat_32bit * boot_record.fat_count;
        off_t const data_sector_count = boot_record.sector_count_32bit - boot_record.reserved_sector_count - fat_sectors;
        fs_info.last_known_free_cluster_count = data_sector_count / boot_record.sectors_per_cluster - 1;
    }
    fs_info.next_free_cluster_hint = boot_record_fat32.root_directory_cluster + 1;
    __builtin_memset(&fs_info.unused2[0], 0, 12);
    fs_info.trailing_signature = 0xAA550000;

    return fs_info;
}

// Note that all I/O is aligned to 512 byte sectors for compatibility with "raw" BSD character-special
// devices. (e.g. /dev/rdisk* on macOS)
static ErrorOr<void> format_fat_16_bit(Core::File& file, FATType fat_type, u64 file_size, u32 volume_id)
{
    VERIFY(fat_type == FATType::FAT12 || fat_type == FATType::FAT16);
    auto boot_record = TRY(generate_dos_3_bios_parameter_block(file_size, fat_type));
    auto boot_record_16_bit = generate_dos_4_bios_parameter_block(fat_type, volume_id);

    Vector<u8> mbr;
    mbr.append(serialize_dos_3_bios_parameter_block(boot_record).data(), sizeof(boot_record));
    mbr.append(serialize_dos_4_bios_parameter_block(boot_record_16_bit).data(), sizeof(boot_record_16_bit));
    mbr.append(&s_bootcode[0], sizeof(s_bootcode));
    mbr.resize(512);
    mbr[510] = s_boot_signature[0];
    mbr[511] = s_boot_signature[1];

    TRY(file.write_until_depleted({ mbr.data(), mbr.size() }));

    Vector<u8> FAT_sector;
    if (fat_type == FATType::FAT12)
        FAT_sector.append(&s_empty_12_bit_fat[0], sizeof(s_empty_12_bit_fat));
    else
        FAT_sector.append(&s_empty_16_bit_fat[0], sizeof(s_empty_16_bit_fat));

    FAT_sector.resize(512);

    for (size_t i = 0; i < boot_record.fat_count; ++i) {
        TRY(file.write_until_depleted({ FAT_sector.data(), FAT_sector.size() }));
        for (u16 j = 0; j < boot_record.sectors_per_fat_16bit - 1; ++j)
            TRY(file.write_until_depleted({ &s_zero_buffer, 512 }));
    }

    size_t root_directory_sectors = ceil_div(boot_record.root_directory_entry_count * s_fat_entry_size, 512);
    for (size_t i = 0; i < root_directory_sectors; ++i)
        TRY(file.write_until_depleted({ &s_zero_buffer, 512 }));

    return {};
}

static ErrorOr<void> format_fat32(Core::File& file, u64 file_size, u32 volume_id)
{
    auto boot_record = TRY(generate_dos_3_bios_parameter_block(file_size, FATType::FAT32));
    auto boot_record_fat32 = generate_dos_7_bios_parameter_block(boot_record, volume_id);
    auto fs_info = generate_fat32_fs_info(boot_record, boot_record_fat32);
    s_bootcode[s_message_offset_offset] = 0x77;

    Vector<u8> mbr;
    mbr.append(serialize_dos_3_bios_parameter_block(boot_record).data(), sizeof(boot_record));
    mbr.append(serialize_dos_7_bios_parameter_block(boot_record_fat32).data(), sizeof(boot_record_fat32));
    mbr.append(&s_bootcode[0], sizeof(s_bootcode));
    mbr.resize(512);
    mbr[510] = s_boot_signature[0];
    mbr[511] = s_boot_signature[1];

    Vector<u8> serialized_fs_info;
    serialized_fs_info.append(serialize_fat32_fs_info(fs_info).data(), sizeof(fs_info));
    VERIFY(serialized_fs_info.size() == 512);

    // Wipe all the reserved sectors.
    for (size_t i = 0; i < boot_record.reserved_sector_count; ++i)
        TRY(file.write_until_depleted({ &s_zero_buffer, 512 }));

    TRY(file.seek(0, SeekMode::SetPosition));

    // Write the boot record and the FSInfo block at the start of the file, and also back them up at sectors 6 and 7 respectively.
    for (size_t i = 0; i < 2; ++i) {
        TRY(file.write_until_depleted({ mbr.data(), mbr.size() }));
        TRY(file.write_until_depleted({ serialized_fs_info.data(), serialized_fs_info.size() }));

        if (i == 0)
            TRY(file.seek(boot_record.bytes_per_sector * 4, SeekMode::FromCurrentPosition));
    }

    Vector<u8> FAT_sector;
    FAT_sector.append(&s_empty_32_bit_fat[0], sizeof(s_empty_32_bit_fat));
    FAT_sector.resize(512);

    TRY(file.seek(boot_record.bytes_per_sector * boot_record.reserved_sector_count, SeekMode::SetPosition));
    for (size_t i = 0; i < boot_record.fat_count; ++i) {
        TRY(file.write_until_depleted({ FAT_sector.data(), FAT_sector.size() }));
        for (size_t j = 0; j < boot_record_fat32.sectors_per_fat_32bit - 1; ++j)
            TRY(file.write_until_depleted({ &s_zero_buffer, 512 }));
    }

    // Erase the root directory cluster (which we always place right after the FAT).
    for (size_t i = 0; i < boot_record.sectors_per_cluster; ++i)
        TRY(file.write_until_depleted({ &s_zero_buffer, 512 }));

    return {};
}

static ErrorOr<int> detect_fat_type_from_file_size(u64 file_size)
{
    u32 sector_count = file_size / 512;

    auto check_table_for_support = [sector_count](auto const& table, bool fat12 = false) {
        for (auto const& potential_entry : table) {
            if (fat12 && sector_count == potential_entry.disk_size)
                return true;
            else if (!fat12 && sector_count <= potential_entry.disk_size)
                return potential_entry.sectors_per_cluster != 0;
        }
        return false;
    };

    if (check_table_for_support(s_disk_table_fat12, true))
        return 12;

    if (file_size < 512 * MiB && check_table_for_support(s_disk_table_fat16))
        return 16;

    if (check_table_for_support(s_disk_table_fat32))
        return 32;

    return Error::from_string_literal("Unable to autodetect a compatible FAT variant");
}

static bool is_valid_fat_type(int fat_type)
{
    return fat_type == 12 || fat_type == 16 || fat_type == 32;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView file_path;
    Optional<int> fat_type_or_empty;

    struct timeval time;
    gettimeofday(&time, NULL);
    u32 volume_id = static_cast<u32>(time.tv_sec) | static_cast<u32>(time.tv_usec);

    Core::ArgsParser args_parser;
    args_parser.add_option(fat_type_or_empty, "FAT type to use, valid types are 12, 16, and 32", "FAT-type", 'F', "FAT type");
    args_parser.add_positional_argument(file_path, "File to format", "file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open(file_path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::DontCreate));

    u64 file_size = 0;
    if (FileSystem::is_device(file->fd()))
        file_size = TRY(FileSystem::block_device_size_from_ioctl(file->fd()));
    else
        file_size = TRY(FileSystem::size_from_fstat(file->fd()));

    int fat_type = 0;
    if (fat_type_or_empty.has_value()) {
        fat_type = fat_type_or_empty.release_value();

        if (!is_valid_fat_type(fat_type))
            return Error::from_string_literal("Invalid FAT type specified, valid types are 12, 16, and 32");
    } else {
        fat_type = TRY(detect_fat_type_from_file_size(file_size));
    }

    switch (fat_type) {
    case 12:
        TRY(format_fat_16_bit(*file, FATType::FAT12, file_size, volume_id));
        break;
    case 16:
        TRY(format_fat_16_bit(*file, FATType::FAT16, file_size, volume_id));
        break;
    case 32:
        TRY(format_fat32(*file, file_size, volume_id));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    sync();

    return 0;
}
