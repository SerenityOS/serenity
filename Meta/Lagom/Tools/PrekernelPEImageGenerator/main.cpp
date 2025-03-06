/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>
#include <time.h>

#include "PEDefinitions.h"

// This tool converts the Prekernel ELF to a PE32+ EFI image.
// It does so by translating the ELF sections into PE sections while keeping the virtual memory layout of the ELF file.
// It also converts ELF R_*_RELATIVE relocations into PE base relocations and adds them as an additional .reloc section.

static constexpr size_t PE_SECTION_ALIGNMENT = 4 * KiB;
static_assert(is_power_of_two(PE_SECTION_ALIGNMENT));

// This is both the minimum and default value for OptionalHeader::WindowsSpecificFields::file_alignment.
static constexpr size_t PE_FILE_ALIGNMENT = 512;
static_assert(is_power_of_two(PE_FILE_ALIGNMENT));

namespace {

ErrorOr<ByteBuffer> translate_relocations(ELF::Image const& elf_image, Span<ELF::Image::Section const*> sorted_elf_sections, Bytes raw_elf)
{
    unsigned none_relocation_type;
    unsigned relative_relocation_type;
    switch (elf_image.machine()) {
    case EM_AARCH64:
        none_relocation_type = R_AARCH64_NONE;
        relative_relocation_type = R_AARCH64_RELATIVE;
        break;
    case EM_RISCV:
        none_relocation_type = R_RISCV_NONE;
        relative_relocation_type = R_RISCV_RELATIVE;
        break;
    case EM_X86_64:
        none_relocation_type = R_X86_64_NONE;
        relative_relocation_type = R_X86_64_RELATIVE;
        break;
    default:
        return Error::from_string_literal("Unsupported e_machine");
    }

    HashMap<u32, Vector<BaseRelocationBlockEntry>> base_relocation_blocks;

    auto add_elf_relocation_to_pe_relocation_blocks = [&](ELF::Image::Relocation const& elf_relocation) {
        if (elf_relocation.type() == none_relocation_type)
            return;
        if (elf_relocation.type() != relative_relocation_type) {
            warnln("Unsupported relocation type: {}", elf_relocation.type());
            VERIFY_NOT_REACHED();
        }

        // Elf_Rela::r_offset is a virtual address for executables, so we need to translate it to an offset in the ELF file.
        FlatPtr* patch_ptr = nullptr;
        for (auto const* elf_section : sorted_elf_sections) {
            if (elf_relocation.offset() >= elf_section->address() && elf_relocation.offset() < elf_section->address() + elf_section->size()) {
                auto relocation_target_offset_in_section = elf_relocation.offset() - elf_section->address();
                patch_ptr = reinterpret_cast<FlatPtr*>(raw_elf.offset(elf_section->offset() + relocation_target_offset_in_section));
            }
        }

        // PE doesn't use addends, so apply the ELF addends now.
        if (elf_relocation.addend_used())
            *patch_ptr = elf_relocation.addend();

        // The PE base relocation table is split into blocks each representing a 4K page.
        u32 page_rva = elf_relocation.offset() & ~0xfff;
        base_relocation_blocks.ensure(page_rva).append(BaseRelocationBlockEntry {
            .offset = static_cast<u16>(elf_relocation.offset() & 0xfff),
            .type = BaseRelocationBlockEntry::Type::DIR64,
        });
    };

    elf_image.for_each_section_of_type(SHT_REL, [&](ELF::Image::Section const& section) {
        auto relocation_section = ELF::Image::RelocationSection(section);
        relocation_section.for_each_relocation([&](ELF::Image::Relocation const& relocation) {
            add_elf_relocation_to_pe_relocation_blocks(relocation);
        });
    });

    elf_image.for_each_section_of_type(SHT_RELA, [&](ELF::Image::Section const& section) {
        auto relocation_section = ELF::Image::RelocationSection(section);
        relocation_section.for_each_relocation([&](ELF::Image::Relocation const& relocation) {
            add_elf_relocation_to_pe_relocation_blocks(relocation);
        });
    });

    AllocatingMemoryStream base_relocation_section_stream;
    for (auto& [page_rva, relocations] : base_relocation_blocks) {
        // Base Relocation Blocks have to be 32-bit aligned, so pad with a (16-bit) ABSOLUTE relocation if needed.
        // PE base relocations of type ABSOLUTE are ignored.
        if (relocations.size() % 2 != 0) {
            auto padding = BaseRelocationBlockEntry {
                .offset = 0,
                .type = BaseRelocationBlockEntry::Type::ABSOLUTE,
            };
            relocations.append(padding);
        }

        auto header = BaseRelocationBlockHeader {
            .page_rva = page_rva,
            .block_size = static_cast<u32>(sizeof(BaseRelocationBlockHeader) + relocations.size() * sizeof(BaseRelocationBlockEntry)),
        };

        TRY(base_relocation_section_stream.write_value(header));

        for (auto const& relocation : relocations)
            TRY(base_relocation_section_stream.write_value(relocation));
    }

    return base_relocation_section_stream.read_until_eof();
}

ErrorOr<COFFHeader> generate_coff_header(ELF::Image const& elf_image, Span<ELF::Image::Section const*> sorted_elf_sections)
{
    COFFHeader::Machine coff_machine;
    switch (elf_image.machine()) {
    case EM_AARCH64:
        coff_machine = COFFHeader::Machine::ARM64;
        break;
    case EM_RISCV:
        coff_machine = COFFHeader::Machine::RISCV64;
        break;
    case EM_X86_64:
        coff_machine = COFFHeader::Machine::AMD64;
        break;
    default:
        return Error::from_string_literal("Unsupported e_machine");
    }

    // +1 for the .reloc section
    u16 pe_section_count = sorted_elf_sections.size() + 1;

    // COFF File Header
    COFFHeader coff_header = {
        .machine = coff_machine,
        .number_of_sections = static_cast<u16>(pe_section_count),
        .time_date_stamp = static_cast<u32>(time(nullptr)),
        .pointer_to_symbol_table = 0,
        .number_of_symbols = 0,
        .size_of_optional_header = sizeof(OptionalHeader),
        .characteristics = COFFHeader::Characteristics::EXECUTABLE_IMAGE | COFFHeader::Characteristics::LINE_NUMS_STRIPPED | COFFHeader::Characteristics::DEBUG_STRIPPED,
    };

    return coff_header;
}

ErrorOr<OptionalHeader> generate_optional_header(ELF::Image const& elf_image, Span<ELF::Image::Section const*> sorted_elf_sections, ReadonlyBytes base_relocation_section_data, u32 size_of_headers)
{
    Optional<u32> base_of_code;
    u32 size_of_code = 0;
    u32 size_of_initialized_data = 0;
    u32 size_of_uninitialized_data = 0;

    // Place the .reloc section after the last ELF section.
    OptionalHeader::DataDirectory reloc_section_data_directory {
        .virtual_address = static_cast<u32>(round_up_to_power_of_two(sorted_elf_sections.last()->address() + sorted_elf_sections.last()->size(), PE_SECTION_ALIGNMENT)),
        .size = static_cast<u32>(base_relocation_section_data.size()),
    };

    for (auto const* elf_section : sorted_elf_sections) {
        if (elf_section->name() == ".reloc"sv)
            return Error::from_string_literal("The Prekernel ELF shouldn't have a .reloc section, as this program will generate it");

        if (elf_section->name().starts_with(".text"sv)) {
            if (!base_of_code.has_value())
                base_of_code = elf_section->address();
            size_of_code += round_up_to_power_of_two(elf_section->size(), PE_FILE_ALIGNMENT);
        } else if (elf_section->name().starts_with(".rdata"sv) || elf_section->name().starts_with(".data"sv)) {
            size_of_initialized_data += round_up_to_power_of_two(elf_section->size(), PE_FILE_ALIGNMENT);
        } else if (elf_section->name().starts_with(".bss"sv)) {
            size_of_uninitialized_data += round_up_to_power_of_two(elf_section->size(), PE_FILE_ALIGNMENT);
        }
    }

    // ImageBase has to be a multiple of 64K.
    Optional<u64> image_base;
    elf_image.for_each_symbol([&image_base](ELF::Image::Symbol const& symbol) {
        if (symbol.name() == "pe_image_base"sv) {
            image_base = symbol.value();
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // We require the PE image base to be zero, so we don't have to subtract it from every address to get the relative virtual addresses.
    VERIFY(image_base.value() == 0);

    // Optional Header
    OptionalHeader optional_header = {
        .standard_fields = {
            .magic = OptionalHeader::StandardFields::Magic::PE32Plus,
            .major_linker_version = 0,
            .minor_linker_version = 0,
            .size_of_code = size_of_code,
            .size_of_initialized_data = size_of_initialized_data,
            .size_of_uninitialized_data = size_of_uninitialized_data,
            .address_of_entry_point = static_cast<u32>(elf_image.entry().get()),
            .base_of_code = base_of_code.release_value(),
        },
        .windows_specific_fields = {
            .image_base = image_base.value(),
            .section_alignment = PE_SECTION_ALIGNMENT,
            .file_alignment = PE_FILE_ALIGNMENT,
            .major_operating_system_version = 0,
            .minor_operating_system_version = 0,
            .major_image_version = 0,
            .minor_image_version = 0,
            .major_subsystem_version = 0,
            .minor_subsystem_version = 0,
            .win32_version_value = 0,
            .size_of_image = reloc_section_data_directory.virtual_address + reloc_section_data_directory.size, // The .reloc section is the last section of the PE image.
            .size_of_headers = size_of_headers,
            .checksum = 0, // The checksum algorithm is not publicly defined. We probably don't need to set it as edk2 PEs built with gcc don't have the checksum set either.
            .subsystem = OptionalHeader::WindowsSpecificFields::Subsystem::EFI_APPLICATION,
            .dll_characteristics = 0,
            .size_of_stack_reserve = 0, // edk2 PEs built with gcc don't have these sizes set, so we probably don't need to set them either.
            .size_of_stack_commit = 0,
            .size_of_heap_reserve = 0,
            .size_of_heap_commit = 0,
            .loader_flags = 0,
            .number_of_rva_and_size = sizeof(OptionalHeader::DataDirectories) / sizeof(u64),
        },
        .data_directories = {
            .export_table = {},
            .import_table = {},
            .resource_table = {},
            .exception_table = {},
            .certificate_table = {},
            .base_relocation_table = reloc_section_data_directory,
            .debug = {},
            .architecture = {},
            .global_ptr = {},
            .tls_table = {},
            .load_config_table = {},
            .bound_import = {},
            .iat = {},
            .delay_import_descriptor = {},
            .clr_runtime_header = {},
            .reserved = {},
        },
    };

    return optional_header;
}

ErrorOr<void> write_pe_headers(Core::OutputBufferedFile& stream, COFFHeader const& coff_header, OptionalHeader const& optional_header)
{
    // MS-DOS Stub
    TRY(stream.write_until_depleted(DOS_MAGIC));

    // Offset to PE signature
    TRY(stream.seek(PE_MAGIC_OFFSET_OFFSET, SeekMode::SetPosition));
    TRY(stream.write_value(LittleEndian<u32> { PE_MAGIC_OFFSET_OFFSET + sizeof(u32) }));

    // Signature
    TRY(stream.write_until_depleted(PE_MAGIC));

    TRY(stream.write_value(coff_header));
    TRY(stream.write_value(optional_header));

    return {};
}

ErrorOr<void> write_pe_sections(Core::OutputBufferedFile& stream, Span<ELF::Image::Section const*> sorted_elf_sections, ReadonlyBytes base_relocation_section_data, COFFHeader const& coff_header, OptionalHeader optional_header)
{
    u32 offset_for_first_raw_data = round_up_to_power_of_two(TRY(stream.tell()) + sizeof(SectionHeader) * coff_header.number_of_sections, PE_FILE_ALIGNMENT);

    Vector<size_t> raw_data_offsets;
    raw_data_offsets.ensure_capacity(sorted_elf_sections.size());

    u32 current_offset_to_raw_data = offset_for_first_raw_data;
    for (auto const* elf_section : sorted_elf_sections) {
        u32 file_size = elf_section->type() == SHT_NOBITS ? 0 : round_up_to_power_of_two(elf_section->size(), PE_FILE_ALIGNMENT);

        auto characteristics = SectionHeader::Characteristics::NONE;

        for (auto const& [special_section_name, special_characteristics] : SPECIAL_PE_SECTIONS) {
            if (special_section_name == elf_section->name()) {
                characteristics = special_characteristics;
                break;
            }
        }

        if (characteristics == SectionHeader::Characteristics::NONE) {
            // Fallback for non-special PE sections
            characteristics |= SectionHeader::Characteristics::MEM_READ;
            if (elf_section->is_writable())
                characteristics |= SectionHeader::Characteristics::MEM_WRITE;
            if (elf_section->is_executable())
                characteristics |= SectionHeader::Characteristics::MEM_EXECUTE | SectionHeader::Characteristics::CNT_CODE;
            characteristics |= file_size == 0 ? SectionHeader::Characteristics::CNT_UNINITIALIZED_DATA : SectionHeader::Characteristics::CNT_INITIALIZED_DATA;
        }

        SectionHeader section_header = {
            .name = {},
            .virtual_size = static_cast<u32>(elf_section->size()),
            .virtual_address = static_cast<u32>(elf_section->address()),
            .size_of_raw_data = file_size,
            .pointer_to_raw_data = file_size == 0 ? 0 : current_offset_to_raw_data,
            .pointer_to_relocations = 0,
            .pointer_to_line_numbers = 0,
            .number_of_relocations = 0,
            .number_of_line_numbers = 0,
            .characteristics = characteristics,
        };

        memset(&section_header.name, 0, sizeof(section_header.name));
        memcpy(&section_header.name, elf_section->name().bytes().data(), min(elf_section->name().bytes().size(), 8));

        TRY(stream.write_value(section_header));

        raw_data_offsets.append(current_offset_to_raw_data);
        current_offset_to_raw_data = round_up_to_power_of_two(current_offset_to_raw_data + file_size, PE_FILE_ALIGNMENT);
    }

    // Also add the section header for the .reloc section.
    SectionHeader reloc_section_header = {
        .name = { ".reloc" },
        .virtual_size = static_cast<u32>(base_relocation_section_data.size()),
        .virtual_address = optional_header.data_directories.base_relocation_table.virtual_address,
        .size_of_raw_data = static_cast<u32>(base_relocation_section_data.size()),
        .pointer_to_raw_data = current_offset_to_raw_data,
        .pointer_to_relocations = 0,
        .pointer_to_line_numbers = 0,
        .number_of_relocations = 0,
        .number_of_line_numbers = 0,
        .characteristics = SectionHeader::Characteristics::CNT_INITIALIZED_DATA | SectionHeader::Characteristics::MEM_READ | SectionHeader::Characteristics::MEM_DISCARDABLE,
    };

    TRY(stream.write_value(reloc_section_header));

    for (size_t section_index = 0; section_index < sorted_elf_sections.size(); section_index++) {
        u32 offset_to_raw_data = raw_data_offsets[section_index];
        auto const* elf_section = sorted_elf_sections[section_index];

        TRY(stream.seek(offset_to_raw_data, SeekMode::SetPosition));
        TRY(stream.write_until_depleted(elf_section->bytes()));
    }

    TRY(stream.seek(reloc_section_header.pointer_to_raw_data, SeekMode::SetPosition));
    TRY(stream.write_until_depleted(base_relocation_section_data));

    return {};
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser argument_parser;
    StringView elf_file_name;
    StringView pe_file_name;
    argument_parser.add_positional_argument(elf_file_name, "Prekernel ELF file", "elf-file", Core::ArgsParser::Required::Yes);
    argument_parser.add_positional_argument(pe_file_name, "Target PE32+ image file", "pe-file", Core::ArgsParser::Required::Yes);
    argument_parser.parse(arguments);

    auto elf_file = TRY(Core::File::open(elf_file_name, Core::File::OpenMode::Read));
    auto elf_data = TRY(elf_file->read_until_eof());
    auto const elf_image = ELF::Image(elf_data.bytes());
    if (!elf_image.is_valid()) {
        return Error::from_string_literal("Invalid ELF passed");
        return -1;
    }

    VERIFY(elf_image.is_executable() || elf_image.is_dynamic());

    if (elf_image.elf_class() != ELFCLASS64)
        return Error::from_string_literal("Unsupported EI_CLASS");

    if (elf_image.byte_order() != ELFDATA2LSB)
        return Error::from_string_literal("Unsupported EI_DATA");

    Vector<ELF::Image::Section> elf_sections;
    elf_sections.ensure_capacity(elf_image.section_count());
    elf_image.for_each_section([&elf_sections, &elf_image](ELF::Image::Section const& elf_section) {
        // We don't support converting RELR relocations.
        VERIFY(elf_section.type() != SHT_RELR);

        // We don't have a runtime to support .{preinit,init,fini}_array sections.
        VERIFY(elf_section.type() != SHT_INIT_ARRAY);
        VERIFY(elf_section.type() != SHT_PREINIT_ARRAY);
        VERIFY(elf_section.type() != SHT_FINI_ARRAY);

        // Don't include some unnecessary sections in the PE image.
        static constexpr Array section_types_to_discard = to_array<u32>({
            SHT_SYMTAB,
            SHT_STRTAB,
            SHT_RELA,
            SHT_HASH,
            SHT_DYNAMIC,
            SHT_NOTE,
            SHT_REL,
            SHT_DYNSYM,
            SHT_GNU_HASH,
        });

        if (elf_image.machine() == EM_RISCV && elf_section.type() == SHT_RISCV_ATTRIBUTES)
            return;

        if (section_types_to_discard.contains_slow(elf_section.type()))
            return;

        // Don't add sections with address 0 or without the ALLOC flag, as they won't appear in memory.
        if (elf_section.address() == 0 || (elf_section.flags() & SHF_ALLOC) == 0)
            return;

        // We keep the memory layout of the ELF sections when translating them to PE sections.
        // The ELF sections therefore have to be properly aligned, as PE sections have to be aligned by the specified amount in WindowsSpecificFields::section_alignment.
        if (elf_section.address() % PE_SECTION_ALIGNMENT != 0) {
            warnln("Prekernel ELF section \"{}\" is not aligned on a {}-byte boundary!", elf_section.name(), PE_SECTION_ALIGNMENT);
            warnln("Either add it to the Prekernel linker script or discard it in the PrekernelPEImageGenerator.");
            VERIFY_NOT_REACHED();
        }

        elf_sections.append(elf_section);
    });

    // PE sections have to be sorted by their virtual_address.
    Vector<ELF::Image::Section const*> sorted_elf_sections;
    sorted_elf_sections.ensure_capacity(elf_sections.size());
    for (auto const& section : elf_sections)
        sorted_elf_sections.append(&section);
    quick_sort(sorted_elf_sections, [](auto const* a, auto const* b) { return a->address() < b->address(); });

    auto output_file = TRY(Core::File::open(pe_file_name, Core::File::OpenMode::Write));
    auto output = TRY(Core::OutputBufferedFile::create(move(output_file)));

    auto base_relocation_section_data = TRY(translate_relocations(elf_image, sorted_elf_sections, elf_data.bytes()));

    auto coff_header = TRY(generate_coff_header(elf_image, sorted_elf_sections));

    u32 size_of_headers = round_up_to_power_of_two(PE_MAGIC_OFFSET_OFFSET + sizeof(u32) + sizeof(PE_MAGIC) + sizeof(COFFHeader) + sizeof(OptionalHeader) + sizeof(SectionHeader) * coff_header.number_of_sections, PE_FILE_ALIGNMENT);

    auto optional_header = TRY(generate_optional_header(elf_image, sorted_elf_sections, base_relocation_section_data, size_of_headers));

    TRY(write_pe_headers(*output, coff_header, optional_header));
    TRY(write_pe_sections(*output, sorted_elf_sections, base_relocation_section_data, coff_header, optional_header));

    return 0;
}
