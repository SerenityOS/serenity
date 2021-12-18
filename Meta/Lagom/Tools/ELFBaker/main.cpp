/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibC/elf.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

template<typename ElfHeader, typename ElfPhdr, typename ElfShdr>
class ElfImage : public ByteBuffer {
public:
    using Header = ElfHeader;
    using ProgramHeader = ElfPhdr;
    using SectionHeader = ElfShdr;

    ElfImage(ByteBuffer& image)
        : m_image(image)
    {
    }

    ElfHeader& get_header()
    {
        return *reinterpret_cast<ElfHeader*>(m_image.data());
    }

    Span<ElfPhdr> get_program_headers()
    {
        auto& header = get_header();
        auto program_headers = reinterpret_cast<ElfPhdr*>(m_image.data() + header.e_phoff);
        return Span<ElfPhdr>(program_headers, header.e_phnum);
    }

    Span<ElfShdr> get_sections()
    {
        auto& header = get_header();
        auto sections = reinterpret_cast<ElfShdr*>(m_image.data() + header.e_shoff);
        return Span<ElfShdr>(sections, header.e_shnum);
    }

private:
    ByteBuffer& m_image;
};

using Elf32Image = ElfImage<Elf32_Ehdr, Elf32_Phdr, Elf32_Shdr>;
using Elf64Image = ElfImage<Elf64_Ehdr, Elf64_Phdr, Elf64_Shdr>;

template<typename T>
class ElfBaker {
public:
    ElfBaker(ByteBuffer& input)
        : m_input_data(input)
        , m_input_image(m_input_data)
        , m_input_header(m_input_image.get_header())
        , m_input_program_headers(m_input_image.get_program_headers())
        , m_input_sections(m_input_image.get_sections())
        , m_program_loads_size(compute_program_load_sizes())
        , m_relocated_sections_size(compute_relocated_sections_size())
    {
    }

    ErrorOr<ByteBuffer> bake()
    {
        TRY(check_eligibility());

        auto& input_header = m_input_image.get_header();
        size_t section_table_size = input_header.e_shnum * input_header.e_shentsize;

        auto result_or_null = ByteBuffer::create_zeroed(m_program_loads_size + section_table_size + m_relocated_sections_size);
        if (!result_or_null.has_value())
            return Error::from_errno(ENOMEM);
        auto& result = result_or_null.value();

        size_t offset = execute_program_headers(result);
        offset += move_section_table(result, offset);
        offset += move_sections(result, offset);

        return result;
    }

private:
    ErrorOr<void> check_eligibility() const
    {
        if (!(m_input_header.e_type == ET_EXEC || m_input_header.e_type == ET_DYN))
            return Error::from_string_literal("Bad ELF type");
        if (m_input_program_headers.is_empty())
            return Error::from_string_literal("No program headers");

        auto& first_program_header = m_input_program_headers[0];
        if (first_program_header.p_type != PT_LOAD)
            return Error::from_string_literal("First program header is not of PT_LOAD type");
        if (first_program_header.p_offset != 0 || first_program_header.p_filesz < sizeof(typename T::Header))
            return Error::from_string_literal("First program header does not contain ELF header, use FILEHDR flag in linker script");
        if (first_program_header.p_filesz > (m_input_header.e_phoff + m_input_header.e_phentsize * m_input_header.e_phnum))
            return Error::from_string_literal("First program header does not contain program header, use PHDRS flag in linker script");

        return {};
    }

    size_t execute_program_headers(ByteBuffer& output)
    {
        T output_image(output);

        for (auto& program_header : m_input_program_headers) {
            if (program_header.p_type == PT_LOAD) {
                auto src_span = m_input_data.span().slice(program_header.p_offset, program_header.p_filesz);
                auto dst_span = output.span().slice(program_header.p_vaddr, program_header.p_filesz);
                src_span.copy_to(dst_span);
            }
        }

        for (auto& program_header : output_image.get_program_headers()) {
            program_header.p_filesz = program_header.p_memsz;
            program_header.p_offset = program_header.p_vaddr;
        }

        return m_program_loads_size;
    }

    size_t move_section_table(ByteBuffer& output, size_t offset)
    {
        T output_image(output);
        auto& output_header = output_image.get_header();
        size_t section_table_size = m_input_header.e_shnum * m_input_header.e_shentsize;

        auto src_span = m_input_data.span().slice(m_input_header.e_shoff, section_table_size);
        auto dst_span = output.span().slice(offset, section_table_size);
        src_span.copy_to(dst_span);

        output_header.e_shoff = offset;

        return section_table_size;
    }

    size_t move_sections(ByteBuffer& output, size_t offset)
    {
        T output_image(output);

        for (auto& section : output_image.get_sections()) {
            if (section.sh_addr == 0 && section.sh_size > 0) {
                auto src_span = m_input_data.span().slice(section.sh_offset, section.sh_size);
                auto dst_span = output.span().slice(offset, section.sh_size);
                src_span.copy_to(dst_span);

                section.sh_offset = offset;

                offset += section.sh_size;
            } else {
                section.sh_offset = section.sh_addr;
            }
        }

        return offset;
    }

    size_t compute_program_load_sizes() const
    {
        size_t total = 0;

        for (auto& program_header : m_input_program_headers) {
            size_t program_end = program_header.p_vaddr + program_header.p_memsz;
            total = max(total, program_end);
        }

        return total;
    }

    size_t compute_relocated_sections_size() const
    {
        size_t total = 0;

        for (auto& section : m_input_sections) {
            if (section.sh_addr == 0 && section.sh_size > 0) {
                total += section.sh_size;
            }
        }

        return total;
    }

    ByteBuffer& m_input_data;
    T m_input_image;
    typename T::Header m_input_header;
    Span<typename T::ProgramHeader> m_input_program_headers;
    Span<typename T::SectionHeader> m_input_sections;
    size_t m_program_loads_size;
    size_t m_relocated_sections_size;
};

using Elf32Baker = ElfBaker<Elf32Image>;
using Elf64Baker = ElfBaker<Elf64Image>;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.argc < 3) {
        warnln("Usage: {} kernel.elf kernel.drow", arguments.argv[0]);
        warnln("Bakes ELF into DROW for usage with prekernel.");
        return 1;
    }

    auto src_file = TRY(Core::File::open(arguments.argv[1], Core::OpenMode::ReadOnly));

    auto src_data = src_file->read_all();
    auto& ehdr = *reinterpret_cast<const Elf32_Ehdr*>(src_data.data());
    if (!IS_ELF(ehdr)) {
        warnln("Error: '{}' is not an ELF file", arguments.argv[1]);
        return 1;
    }

    ByteBuffer output;
    switch (ehdr.e_ident[EI_CLASS]) {
    case ELFCLASS32:
        output = TRY(Elf32Baker(src_data).bake());
        break;
    case ELFCLASS64:
        output = TRY(Elf64Baker(src_data).bake());
        break;
    default:
        warnln("Error: '{}' has an unknown ELF class", arguments.argv[1]);
        return 1;
    }

    auto dst_file = TRY(Core::File::open(arguments.argv[2], Core::OpenMode::Truncate | Core::OpenMode::WriteOnly));
    if (!dst_file->write(output.data(), output.size())) {
        warnln("Error: Failed to write '{}'", arguments.argv[2]);
        return 1;
    }

    return 0;
}
