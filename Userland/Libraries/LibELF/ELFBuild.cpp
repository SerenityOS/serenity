/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibELF/ELFBuild.h>
namespace ELF {

SectionTable::Index StringTable::emit_into_builder(u32 name_index, SectionTable& builder) const noexcept
{
    return builder.append(emit_section(name_index));
}

Section StringTable::emit_section(u32 name_index) const noexcept
{
    Elf64_Shdr header {};
    header.sh_name = name_index;
    header.sh_type = SHT_STRTAB;
    header.sh_flags = 0;
    header.sh_addr = 0;
    header.sh_link = 0;
    header.sh_info = 0;
    header.sh_entsize = 0;
    header.sh_addralign = 0;
    return Section(m_data.span(), header);
}

u32 StringTable::insert(StringView str) noexcept
{
    // The offsets for sh_name and st_name are 32-bit unsigned integers, so it
    // won't make sense to address a string table bigger than what u32 can
    // provide.
    VERIFY(m_data.size() < NumericLimits<u32>::max());
    auto const offset = static_cast<u32>(m_data.size());

    auto const final_size = m_data.size() + str.length() + 1;
    VERIFY(final_size < NumericLimits<u32>::max());

    m_data.ensure_capacity(m_data.size() + str.length() + 1);

    for (auto ch : str) {
        VERIFY(ch != 0);
        m_data.unchecked_append(ch);
    }
    m_data.append(0);
    return offset;
}

FixedArray<u8> build_elf_image(u64 shstrndx, Elf64_Quarter image_type, ReadonlySpan<Section> sections)
{
    Checked<u64> final_image_size = sizeof(Elf64_Ehdr);
    Vector<u64> section_offsets;
    section_offsets.ensure_capacity(sections.size());

    auto const sections_begin = final_image_size.value_unchecked();
    final_image_size += sizeof(Elf64_Shdr) * sections.size();

    for (auto const& section : sections) {
        auto const offset = final_image_size.value();
        section_offsets.unchecked_append(offset);
        if (section.data.has_value()) {
            final_image_size += section.data.value().size();
        }
    }

    auto image = MUST(FixedArray<u8>::create(final_image_size.value()));

    {
        auto section_headers = Span<Elf64_Shdr> {
            reinterpret_cast<Elf64_Shdr*>(image.span().offset_pointer(sections_begin)),
            sections.size(),
        };
        for (size_t i = 0; i < sections.size(); ++i) {
            section_headers[i] = sections[i].header;
            section_headers[i].sh_offset = section_offsets[i];
            if (sections[i].data.has_value()) {
                auto const data = sections[i].data.value();
                auto const data_in_elf = image.span().slice(section_offsets[i], data.size());
                data.copy_to(data_in_elf);
                section_headers[i].sh_size = data.size();
            }
        }
    }

    {
        auto* const final_elf_hdr = reinterpret_cast<Elf64_Ehdr*>(image.data());
        final_elf_hdr->e_ident[EI_MAG0] = 0x7f;
        final_elf_hdr->e_ident[EI_MAG1] = 'E';
        final_elf_hdr->e_ident[EI_MAG2] = 'L';
        final_elf_hdr->e_ident[EI_MAG3] = 'F';
        final_elf_hdr->e_ident[EI_CLASS] = ELFCLASS64;
        // FIXME: This is platform-dependent. Any big-endian host will write the
        // data in MSB format, so the EI_DATA field should be set to
        // ELFDATA2MSB.
        final_elf_hdr->e_ident[EI_DATA] = ELFDATA2LSB;
        final_elf_hdr->e_ident[EI_VERSION] = EV_CURRENT;
        // FIXME: This is platform-dependent. The host must set the OSABI to the
        // one of the image target.
        final_elf_hdr->e_ident[EI_OSABI] = ELFOSABI_SYSV;
        final_elf_hdr->e_ident[EI_ABIVERSION] = 0;
        auto padding = Bytes {
            &final_elf_hdr->e_ident[EI_PAD],
            EI_NIDENT - EI_PAD,
        };
        padding.fill(0);

        final_elf_hdr->e_type = image_type;
        // FIXME: This is platform-dependent. This must be set to the host
        // architecture.
        final_elf_hdr->e_machine = EM_AMD64;
        final_elf_hdr->e_version = EV_CURRENT;

        // Currently segments aren't supported, hence no program headers.
        // FIXME: Update program header info on ELF header when adding segment
        // information.
        final_elf_hdr->e_phoff = 0;
        final_elf_hdr->e_phnum = 0;
        final_elf_hdr->e_phentsize = 0;

        final_elf_hdr->e_shoff = sections_begin;
        final_elf_hdr->e_shnum = sections.size();
        final_elf_hdr->e_shentsize = sizeof(Section::header);

        // FIXME: This is platform-dependent. The flags field should be in sync
        // with the architecture flags assumed in the code sections, otherwise
        // instructions may be misinterpreted.
        final_elf_hdr->e_flags = 0;

        final_elf_hdr->e_ehsize = sizeof(*final_elf_hdr);
        final_elf_hdr->e_shstrndx = shstrndx;
    }

    return image;
}
};
