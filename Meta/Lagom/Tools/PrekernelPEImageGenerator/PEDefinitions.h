/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/EnumBits.h>
#include <AK/StringView.h>
#include <AK/Types.h>

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format

// NOTE: All struct definitions in this file assume little endian.
//       Only PE32+ (64 bit) images are supported.

// The PE file offset to the value containing the offset of the PE magic.
static constexpr size_t PE_MAGIC_OFFSET_OFFSET = 0x3c;

static constexpr Array<u8, 2> DOS_MAGIC = { 'M', 'Z' };
static constexpr Array<u8, 4> PE_MAGIC = { 'P', 'E', '\0', '\0' };

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#coff-file-header-object-and-image
struct [[gnu::packed]] COFFHeader {
    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#machine-types
    enum class Machine : u16 {
        AMD64 = 0x8664,
        ARM64 = 0xaa64,
        RISCV64 = 0x5064,
    };

    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#characteristics
    enum class Characteristics : u16 {
        RELOCS_STRIPPED = 0x0001,
        EXECUTABLE_IMAGE = 0x0002,
        LINE_NUMS_STRIPPED = 0x0004,
        LOCAL_SYMS_STRIPPED = 0x0008,
        AGGRESSIVE_WS_TRIM = 0x0010,
        LARGE_ADDRESS_AWARE = 0x0020,
        BYTES_REVERSED_LO = 0x0080,
        _32BIT_MACHINE = 0x0100,
        DEBUG_STRIPPED = 0x0200,
        REMOVABLE_RUN_FROM_SWAP = 0x0400,
        NET_RUN_FROM_SWAP = 0x0800,
        IMAGE_FILE_SYSTEM = 0x1000,
        DLL = 0x2000,
        UP_SYSTEM_ONLY = 0x4000,
        BYTES_REVERSED_HI = 0x8000,
    };

    Machine machine;
    u16 number_of_sections;
    u32 time_date_stamp; // 32-bit time_t :^(
    u32 pointer_to_symbol_table;
    u32 number_of_symbols;
    u16 size_of_optional_header;
    Characteristics characteristics;
};
static_assert(AssertSize<COFFHeader, 20>());
AK_ENUM_BITWISE_OPERATORS(COFFHeader::Characteristics)

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#optional-header-image-only
struct [[gnu::packed]] OptionalHeader {
    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#optional-header-standard-fields-image-only
    struct [[gnu::packed]] StandardFields {
        enum class Magic : u16 {
            PE32 = 0x10b,
            PE32Plus = 0x20b,
        };

        Magic magic;
        u8 major_linker_version;
        u8 minor_linker_version;
        u32 size_of_code;
        u32 size_of_initialized_data;
        u32 size_of_uninitialized_data;
        u32 address_of_entry_point;
        u32 base_of_code;
    };
    static_assert(AssertSize<StandardFields, 24>());

    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#optional-header-windows-specific-fields-image-only
    struct [[gnu::packed]] WindowsSpecificFields {
        // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#windows-subsystem
        enum class Subsystem : u16 {
            EFI_APPLICATION = 10,
        };

        u64 image_base;
        u32 section_alignment;
        u32 file_alignment;
        u16 major_operating_system_version;
        u16 minor_operating_system_version;
        u16 major_image_version;
        u16 minor_image_version;
        u16 major_subsystem_version;
        u16 minor_subsystem_version;
        u32 win32_version_value;
        u32 size_of_image;
        u32 size_of_headers;
        u32 checksum;
        Subsystem subsystem;
        u16 dll_characteristics;
        u64 size_of_stack_reserve;
        u64 size_of_stack_commit;
        u64 size_of_heap_reserve;
        u64 size_of_heap_commit;
        u32 loader_flags;
        u32 number_of_rva_and_size;
    };
    static_assert(AssertSize<WindowsSpecificFields, 112 - 24>());

    struct [[gnu::packed]] DataDirectory {
        u32 virtual_address;
        u32 size;
    };
    static_assert(AssertSize<DataDirectory, 8>());

    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#optional-header-data-directories-image-only
    struct [[gnu::packed]] DataDirectories {
        DataDirectory export_table;
        DataDirectory import_table;
        DataDirectory resource_table;
        DataDirectory exception_table;
        DataDirectory certificate_table;
        DataDirectory base_relocation_table;
        DataDirectory debug;
        DataDirectory architecture;
        DataDirectory global_ptr;
        DataDirectory tls_table;
        DataDirectory load_config_table;
        DataDirectory bound_import;
        DataDirectory iat;
        DataDirectory delay_import_descriptor;
        DataDirectory clr_runtime_header;
        DataDirectory reserved;
    };
    static_assert(AssertSize<DataDirectories, 240 - 112>());

    StandardFields standard_fields;
    WindowsSpecificFields windows_specific_fields;
    DataDirectories data_directories;
};
static_assert(AssertSize<OptionalHeader, 240>());

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#section-table-section-headers
struct [[gnu::packed]] SectionHeader {
    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#section-flags
    enum class Characteristics : u32 {
        NONE = 0x00000000,
        TYPE_NO_PAD = 0x00000008,
        CNT_CODE = 0x00000020,
        CNT_INITIALIZED_DATA = 0x00000040,
        CNT_UNINITIALIZED_DATA = 0x00000080,
        LNK_OTHER = 0x00000100,
        LNK_INFO = 0x00000200,
        LNK_REMOVE = 0x00000800,
        LNK_COMDAT = 0x00001000,
        GPREL = 0x00008000,
        MEM_PURGEABLE = 0x00020000,
        MEM_16BIT = 0x00020000,
        MEM_LOCKED = 0x00040000,
        MEM_PRELOAD = 0x00080000,
        ALIGN_1BYTES = 0x00100000,
        ALIGN_2BYTES = 0x00200000,
        ALIGN_4BYTES = 0x00300000,
        ALIGN_8BYTES = 0x00400000,
        ALIGN_16BYTES = 0x00500000,
        ALIGN_32BYTES = 0x00600000,
        ALIGN_64BYTES = 0x00700000,
        ALIGN_128BYTES = 0x00800000,
        ALIGN_256BYTES = 0x00900000,
        ALIGN_512BYTES = 0x00a00000,
        ALIGN_1024BYTES = 0x00b00000,
        ALIGN_2048BYTES = 0x00c00000,
        ALIGN_4096BYTES = 0x00d00000,
        ALIGN_8192BYTES = 0x00e00000,
        LNK_NRELOC_OVFL = 0x01000000,
        MEM_DISCARDABLE = 0x02000000,
        MEM_NOT_CACHED = 0x04000000,
        MEM_NOT_PAGED = 0x08000000,
        MEM_SHARED = 0x10000000,
        MEM_EXECUTE = 0x20000000,
        MEM_READ = 0x40000000,
        MEM_WRITE = 0x80000000,
    };

    Array<char, 8> name;
    u32 virtual_size;
    u32 virtual_address;
    u32 size_of_raw_data;
    u32 pointer_to_raw_data;
    u32 pointer_to_relocations;
    u32 pointer_to_line_numbers;
    u16 number_of_relocations;
    u16 number_of_line_numbers;
    Characteristics characteristics;
};
static_assert(AssertSize<SectionHeader, 40>());
AK_ENUM_BITWISE_OPERATORS(SectionHeader::Characteristics)

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#special-sections
struct SpecialSection {
    StringView name;
    SectionHeader::Characteristics characteristics;
};

// This array only includes special sections that may be in the Prekernel ELF.
static constexpr Array SPECIAL_PE_SECTIONS = to_array<SpecialSection>({
    { ".bss"sv, SectionHeader::Characteristics::CNT_UNINITIALIZED_DATA | SectionHeader::Characteristics::MEM_READ | SectionHeader::Characteristics::MEM_WRITE },
    { ".data"sv, SectionHeader::Characteristics::CNT_INITIALIZED_DATA | SectionHeader::Characteristics::MEM_READ | SectionHeader::Characteristics::MEM_WRITE },
    { ".rdata"sv, SectionHeader::Characteristics::CNT_INITIALIZED_DATA | SectionHeader::Characteristics::MEM_READ },
    { ".text"sv, SectionHeader::Characteristics::CNT_CODE | SectionHeader::Characteristics::MEM_EXECUTE | SectionHeader::Characteristics::MEM_READ },
});

// https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#base-relocation-block
struct [[gnu::packed]] BaseRelocationBlockHeader {
    u32 page_rva;
    u32 block_size;
};
static_assert(AssertSize<BaseRelocationBlockHeader, 8>());

struct [[gnu::packed]] BaseRelocationBlockEntry {
    // https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#base-relocation-types
    enum class Type : u16 {
        ABSOLUTE = 0,
        DIR64 = 10,
    };

    u16 offset : 12;
    Type type : 4;
};
static_assert(AssertSize<BaseRelocationBlockEntry, 2>());

template<>
struct AK::Traits<COFFHeader> : public AK::DefaultTraits<COFFHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<OptionalHeader> : public AK::DefaultTraits<OptionalHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<SectionHeader> : public AK::DefaultTraits<SectionHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<BaseRelocationBlockHeader> : public AK::DefaultTraits<BaseRelocationBlockHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<BaseRelocationBlockEntry> : public AK::DefaultTraits<BaseRelocationBlockEntry> {
    static constexpr bool is_trivially_serializable() { return true; }
};
