#include <AK/StringBuilder.h>
#include <AK/kstdio.h>
#include <LibELF/ELFImage.h>

ELFImage::ELFImage(const u8* buffer, size_t size)
    : m_buffer(buffer)
    , m_size(size)
{
    m_valid = parse();
}

ELFImage::~ELFImage()
{
}

static const char* object_file_type_to_string(Elf32_Half type)
{
    switch (type) {
    case ET_NONE:
        return "None";
    case ET_REL:
        return "Relocatable";
    case ET_EXEC:
        return "Executable";
    case ET_DYN:
        return "Shared object";
    case ET_CORE:
        return "Core";
    default:
        return "(?)";
    }
}

const char* ELFImage::section_index_to_string(unsigned index) const
{
    if (index == SHN_UNDEF)
        return "Undefined";
    if (index >= SHN_LORESERVE)
        return "Reserved";
    return section(index).name();
}

unsigned ELFImage::symbol_count() const
{
    return section(m_symbol_table_section_index).entry_count();
}

void ELFImage::dump() const
{
    dbgprintf("ELFImage{%p} {\n", this);
    dbgprintf("    is_valid: %u\n", is_valid());

    if (!is_valid()) {
        dbgprintf("}\n");
        return;
    }

    dbgprintf("    type:    %s\n", object_file_type_to_string(header().e_type));
    dbgprintf("    machine: %u\n", header().e_machine);
    dbgprintf("    entry:   %x\n", header().e_entry);
    dbgprintf("    shoff:   %u\n", header().e_shoff);
    dbgprintf("    shnum:   %u\n", header().e_shnum);
    dbgprintf("    phoff:   %u\n", header().e_phoff);
    dbgprintf("    phnum:   %u\n", header().e_phnum);
    dbgprintf(" shstrndx:   %u\n", header().e_shstrndx);

    for_each_program_header([&](const ProgramHeader& program_header) {
        dbgprintf("    Program Header %d: {\n", program_header.index());
        dbgprintf("        type: %x\n", program_header.type());
        dbgprintf("      offset: %x\n", program_header.offset());
        dbgprintf("       flags: %x\n", program_header.flags());
        dbgprintf("        \n");
        dbgprintf("    }\n");
    });

    for (unsigned i = 0; i < header().e_shnum; ++i) {
        auto& section = this->section(i);
        dbgprintf("    Section %u: {\n", i);
        dbgprintf("        name: %s\n", section.name());
        dbgprintf("        type: %x\n", section.type());
        dbgprintf("      offset: %x\n", section.offset());
        dbgprintf("        size: %u\n", section.size());
        dbgprintf("        \n");
        dbgprintf("    }\n");
    }

    dbgprintf("Symbol count: %u (table is %u)\n", symbol_count(), m_symbol_table_section_index);
    for (unsigned i = 1; i < symbol_count(); ++i) {
        auto& sym = symbol(i);
        dbgprintf("Symbol @%u:\n", i);
        dbgprintf("    Name: %s\n", sym.name());
        dbgprintf("    In section: %s\n", section_index_to_string(sym.section_index()));
        dbgprintf("    Value: %x\n", sym.value());
        dbgprintf("    Size: %u\n", sym.size());
    }

    dbgprintf("}\n");
}

unsigned ELFImage::section_count() const
{
    return header().e_shnum;
}

unsigned ELFImage::program_header_count() const
{
    return header().e_phnum;
}

bool ELFImage::parse()
{
    if (!validate_elf_header(header(), m_size)) {
        dbgputstr("ELFImage::parse(): ELF Header not valid\n");
        return false;
    }

    // First locate the string tables.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& sh = section_header(i);
        if (sh.sh_type == SHT_SYMTAB) {
            ASSERT(!m_symbol_table_section_index || m_symbol_table_section_index == i);
            m_symbol_table_section_index = i;
        }
        if (sh.sh_type == SHT_STRTAB && i != header().e_shstrndx) {
            if (StringView(".strtab") == section_header_table_string(sh.sh_name))
                m_string_table_section_index = i;
        }
    }

    // Then create a name-to-index map.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& section = this->section(i);
        m_sections.set(section.name(), move(i));
    }

    return true;
}

const char* ELFImage::section_header_table_string(unsigned offset) const
{
    auto& sh = section_header(header().e_shstrndx);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return raw_data(sh.sh_offset + offset);
}

const char* ELFImage::table_string(unsigned offset) const
{
    auto& sh = section_header(m_string_table_section_index);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return raw_data(sh.sh_offset + offset);
}

const char* ELFImage::raw_data(unsigned offset) const
{
    return reinterpret_cast<const char*>(m_buffer) + offset;
}

const Elf32_Ehdr& ELFImage::header() const
{
    return *reinterpret_cast<const Elf32_Ehdr*>(raw_data(0));
}

const Elf32_Phdr& ELFImage::program_header_internal(unsigned index) const
{
    ASSERT(index < header().e_phnum);
    return *reinterpret_cast<const Elf32_Phdr*>(raw_data(header().e_phoff + (index * sizeof(Elf32_Phdr))));
}

const Elf32_Shdr& ELFImage::section_header(unsigned index) const
{
    ASSERT(index < header().e_shnum);
    return *reinterpret_cast<const Elf32_Shdr*>(raw_data(header().e_shoff + (index * header().e_shentsize)));
}

const ELFImage::Symbol ELFImage::symbol(unsigned index) const
{
    ASSERT(index < symbol_count());
    auto* raw_syms = reinterpret_cast<const Elf32_Sym*>(raw_data(section(m_symbol_table_section_index).offset()));
    return Symbol(*this, index, raw_syms[index]);
}

const ELFImage::Section ELFImage::section(unsigned index) const
{
    ASSERT(index < section_count());
    return Section(*this, index);
}

const ELFImage::ProgramHeader ELFImage::program_header(unsigned index) const
{
    ASSERT(index < program_header_count());
    return ProgramHeader(*this, index);
}

const ELFImage::Relocation ELFImage::RelocationSection::relocation(unsigned index) const
{
    ASSERT(index < relocation_count());
    auto* rels = reinterpret_cast<const Elf32_Rel*>(m_image.raw_data(offset()));
    return Relocation(m_image, rels[index]);
}

const ELFImage::RelocationSection ELFImage::Section::relocations() const
{
    StringBuilder builder;
    builder.append(".rel");
    builder.append(name());

    auto relocation_section = m_image.lookup_section(builder.to_string());
    if (relocation_section.type() != SHT_REL)
        return static_cast<const RelocationSection>(m_image.section(0));

#ifdef ELFIMAGE_DEBUG
    dbgprintf("Found relocations for %s in %s\n", name(), relocation_section.name());
#endif
    return static_cast<const RelocationSection>(relocation_section);
}

const ELFImage::Section ELFImage::lookup_section(const String& name) const
{
    if (auto it = m_sections.find(name); it != m_sections.end())
        return section((*it).value);
    return section(0);
}

bool ELFImage::validate_elf_header(const Elf32_Ehdr& elf_header, size_t file_size)
{
    if (!IS_ELF(elf_header)) {
        dbgputstr("File is not an ELF file.\n");
        return false;
    }

    if (ELFCLASS32 != elf_header.e_ident[EI_CLASS]) {
        dbgputstr("File is not a 32 bit ELF file.\n");
        return false;
    }

    if (ELFDATA2LSB != elf_header.e_ident[EI_DATA]) {
        dbgputstr("File is not a little endian ELF file.\n");
        return false;
    }

    if (EV_CURRENT != elf_header.e_ident[EI_VERSION]) {
        dbgprintf("File has unrecognized ELF version (%d), expected (%d)!\n", elf_header.e_ident[EI_VERSION], EV_CURRENT);
        return false;
    }

    if (ELFOSABI_SYSV != elf_header.e_ident[EI_OSABI]) {
        dbgprintf("File has unknown OS ABI (%d), expected SYSV(0)!\n", elf_header.e_ident[EI_OSABI]);
        return false;
    }

    if (0 != elf_header.e_ident[EI_ABIVERSION]) {
        dbgprintf("File has unknown SYSV ABI version (%d)!\n", elf_header.e_ident[EI_ABIVERSION]);
        return false;
    }

    if (EM_386 != elf_header.e_machine) {
        dbgprintf("File has unknown machine (%d), expected i386 (3)!\n", elf_header.e_machine);
        return false;
    }

    if (ET_EXEC != elf_header.e_type && ET_DYN != elf_header.e_type && ET_REL != elf_header.e_type) {
        dbgprintf("File has unloadable ELF type (%d), expected REL (1), EXEC (2) or DYN (3)!\n", elf_header.e_type);
        return false;
    }

    if (EV_CURRENT != elf_header.e_version) {
        dbgprintf("File has unrecognized ELF version (%d), expected (%d)!\n", elf_header.e_version, EV_CURRENT);
        return false;
    }

    if (sizeof(Elf32_Ehdr) != elf_header.e_ehsize) {
        dbgprintf("File has incorrect ELF header size..? (%d), expected (%d)!\n", elf_header.e_ehsize, sizeof(Elf32_Ehdr));
        return false;
    }

    if (elf_header.e_phoff > file_size || elf_header.e_shoff > file_size) {
        dbgprintf("SHENANIGANS! program header offset (%d) or section header offset (%d) are past the end of the file!\n",
            elf_header.e_phoff, elf_header.e_shoff);
        return false;
    }

    if (elf_header.e_phnum != 0 && elf_header.e_phoff != elf_header.e_ehsize) {
        dbgprintf("File does not have program headers directly after the ELF header? program header offset (%d), expected (%d).\n",
            elf_header.e_phoff, elf_header.e_ehsize);
        return false;
    }

    if (0 != elf_header.e_flags) {
        dbgprintf("File has incorrect ELF header flags...? (%d), expected (%d).\n", elf_header.e_flags, 0);
        return false;
    }

    if (0 != elf_header.e_phnum && sizeof(Elf32_Phdr) != elf_header.e_phentsize) {
        dbgprintf("File has incorrect program header size..? (%d), expected (%d).\n", elf_header.e_phentsize, sizeof(Elf32_Phdr));
        return false;
    }

    if (sizeof(Elf32_Shdr) != elf_header.e_shentsize) {
        dbgprintf("File has incorrect section header size..? (%d), expected (%d).\n", elf_header.e_shentsize, sizeof(Elf32_Shdr));
        return false;
    }

    size_t end_of_last_program_header = elf_header.e_phoff + (elf_header.e_phnum * elf_header.e_phentsize);
    if (end_of_last_program_header > file_size) {
        dbgprintf("SHENANIGANS! End of last program header (%d) is past the end of the file!\n", end_of_last_program_header);
        return false;
    }

    size_t end_of_last_section_header = elf_header.e_shoff + (elf_header.e_shnum * elf_header.e_shentsize);
    if (end_of_last_section_header > file_size) {
        dbgprintf("SHENANIGANS! End of last section header (%d) is past the end of the file!\n", end_of_last_section_header);
        return false;
    }

    if (elf_header.e_shstrndx >= elf_header.e_shnum) {
        dbgprintf("SHENANIGANS! Section header string table index (%d) is not a valid index given we have %d section headers!\n", elf_header.e_shstrndx, elf_header.e_shnum);
        return false;
    }

    return true;
}

bool ELFImage::validate_program_headers(const Elf32_Ehdr& elf_header, size_t file_size, u8* buffer, size_t buffer_size, String& interpreter_path)
{
    // Can we actually parse all the program headers in the given buffer?
    size_t end_of_last_program_header = elf_header.e_phoff + (elf_header.e_phnum * elf_header.e_phentsize);
    if (end_of_last_program_header > buffer_size) {
        dbgprintf("Unable to parse program headers from buffer, buffer too small! Buffer size: %zu, End of program headers %zu\n",
            buffer_size, end_of_last_program_header);
        return false;
    }

    if (file_size < buffer_size) {
        dbgputstr("We somehow read more from a file than was in the file in the first place!\n");
        ASSERT_NOT_REACHED();
    }

    size_t num_program_headers = elf_header.e_phnum;
    auto program_header_begin = (const Elf32_Phdr*)&(buffer[elf_header.e_phoff]);

    for (size_t header_index = 0; header_index < num_program_headers; ++header_index) {
        auto& program_header = program_header_begin[header_index];
        switch (program_header.p_type) {
        case PT_INTERP:
            if (ET_DYN != elf_header.e_type) {
                dbgprintf("Found PT_INTERP header (%d) in non-DYN ELF object! What? We can't handle this!\n", header_index);
                return false;
            }
            // We checked above that file_size was >= buffer size. We only care about buffer size anyway, we're trying to read this!
            if (program_header.p_offset + program_header.p_filesz > buffer_size) {
                dbgprintf("Found PT_INTERP header (%d), but the .interp section was not within our buffer :( Your program will not be loaded today.\n", header_index);
                return false;
            }
            interpreter_path = String((const char*)&buffer[program_header.p_offset], program_header.p_filesz - 1);
            break;
        case PT_LOAD:
        case PT_DYNAMIC:
        case PT_NOTE:
        case PT_PHDR:
        case PT_TLS:
            if (program_header.p_offset + program_header.p_filesz > file_size) {
                dbgprintf("SHENANIGANS! Program header %d segment leaks beyond end of file!\n", header_index);
                return false;
            }
            if ((program_header.p_flags & PF_X) && (program_header.p_flags & PF_W)) {
                dbgprintf("SHENANIGANS! Program header %d segment is marked write and execute\n", header_index);
                return false;
            }
            break;
        default:
            // Not handling other program header types in other code so... let's not surprise them
            dbgprintf("Found program header (%d) of unrecognized type %d!\n", header_index, program_header.p_type);
            ASSERT_NOT_REACHED();
            break;
        }
    }
    return true;
}
