#include <LibELF/ELFDynamicObject.h>
#include <LibELF/exec_elf.h>

#include <AK/StringBuilder.h>

#include <assert.h>
#include <stdio.h>

static const char* name_for_dtag(Elf32_Sword d_tag);

ELFDynamicObject::ELFDynamicObject(VirtualAddress base_address, VirtualAddress dynamic_section_addresss)
    : m_base_address(base_address)
    , m_dynamic_address(dynamic_section_addresss)
{
    parse();
}

ELFDynamicObject::~ELFDynamicObject()
{
}

void ELFDynamicObject::dump() const
{
    StringBuilder builder;
    builder.append("\nd_tag      tag_name         value\n");
    size_t num_dynamic_sections = 0;

    for_each_dynamic_entry([&](const ELFDynamicObject::DynamicEntry& entry) {
        String name_field = String::format("(%s)", name_for_dtag(entry.tag()));
        builder.appendf("0x%08X %-17s0x%X\n", entry.tag(), name_field.characters(), entry.val());
        num_dynamic_sections++;
        return IterationDecision::Continue;
    });

    dbgprintf("Dynamic section at address 0x%x contains %zu entries:\n", m_dynamic_address.as_ptr(), num_dynamic_sections);
    dbgprintf(builder.to_string().characters());
}

void ELFDynamicObject::parse()
{
    for_each_dynamic_entry([&](const DynamicEntry& entry) {
        switch (entry.tag()) {
        case DT_INIT:
            m_init_offset = entry.ptr();
            break;
        case DT_FINI:
            m_fini_offset = entry.ptr();
            break;
        case DT_INIT_ARRAY:
            m_init_array_offset = entry.ptr();
            break;
        case DT_INIT_ARRAYSZ:
            m_init_array_size = entry.val();
            break;
        case DT_FINI_ARRAY:
            m_fini_array_offset = entry.ptr();
            break;
        case DT_FINI_ARRAYSZ:
            m_fini_array_size = entry.val();
            break;
        case DT_HASH:
            m_hash_table_offset = entry.ptr();
            break;
        case DT_SYMTAB:
            m_symbol_table_offset = entry.ptr();
            break;
        case DT_STRTAB:
            m_string_table_offset = entry.ptr();
            break;
        case DT_STRSZ:
            m_size_of_string_table = entry.val();
            break;
        case DT_SYMENT:
            m_size_of_symbol_table_entry = entry.val();
            break;
        case DT_PLTGOT:
            m_procedure_linkage_table_offset = entry.ptr();
            break;
        case DT_PLTRELSZ:
            m_size_of_plt_relocation_entry_list = entry.val();
            break;
        case DT_PLTREL:
            m_procedure_linkage_table_relocation_type = entry.val();
            ASSERT(m_procedure_linkage_table_relocation_type & (DT_REL | DT_RELA));
            break;
        case DT_JMPREL:
            m_plt_relocation_offset_location = entry.ptr();
            break;
        case DT_RELA:
        case DT_REL:
            m_relocation_table_offset = entry.ptr();
            break;
        case DT_RELASZ:
        case DT_RELSZ:
            m_size_of_relocation_table = entry.val();
            break;
        case DT_RELAENT:
        case DT_RELENT:
            m_size_of_relocation_entry = entry.val();
            break;
        case DT_RELACOUNT:
        case DT_RELCOUNT:
            m_number_of_relocations = entry.val();
            break;
        case DT_FLAGS:
            m_dt_flags = entry.val();
            break;
        case DT_TEXTREL:
            m_dt_flags |= DF_TEXTREL; // This tag seems to exist for legacy reasons only?
            break;
        default:
            dbgprintf("ELFDynamicObject: DYNAMIC tag handling not implemented for DT_%s\n", name_for_dtag(entry.tag()));
            printf("ELFDynamicObject: DYNAMIC tag handling not implemented for DT_%s\n", name_for_dtag(entry.tag()));
            ASSERT_NOT_REACHED(); // FIXME: Maybe just break out here and return false?
            break;
        }
        return IterationDecision::Continue;
    });

    auto hash_section_address = hash_section().address().as_ptr();
    auto num_hash_chains = ((u32*)hash_section_address)[1];
    m_symbol_count = num_hash_chains;
}

const ELFDynamicObject::Relocation ELFDynamicObject::RelocationSection::relocation(unsigned index) const
{
    ASSERT(index < entry_count());
    unsigned offset_in_section = index * entry_size();
    auto relocation_address = (Elf32_Rel*)address().offset(offset_in_section).as_ptr();
    return Relocation(m_dynamic, *relocation_address, offset_in_section);
}

const ELFDynamicObject::Relocation ELFDynamicObject::RelocationSection::relocation_at_offset(unsigned offset) const
{
    ASSERT(offset <= (m_section_size_bytes - m_entry_size));
    auto relocation_address = (Elf32_Rel*)address().offset(offset).as_ptr();
    return Relocation(m_dynamic, *relocation_address, offset);
}

const ELFDynamicObject::Symbol ELFDynamicObject::symbol(unsigned index) const
{
    auto symbol_section = Section(*this, m_symbol_table_offset, (m_symbol_count * m_size_of_symbol_table_entry), m_size_of_symbol_table_entry, "DT_SYMTAB");
    auto symbol_entry = (Elf32_Sym*)symbol_section.address().offset(index * symbol_section.entry_size()).as_ptr();
    return Symbol(*this, index, *symbol_entry);
}

const ELFDynamicObject::Section ELFDynamicObject::init_section() const
{
    return Section(*this, m_init_offset, sizeof(void (*)()), sizeof(void (*)()), "DT_INIT");
}

const ELFDynamicObject::Section ELFDynamicObject::fini_section() const
{
    return Section(*this, m_fini_offset, sizeof(void (*)()), sizeof(void (*)()), "DT_FINI");
}

const ELFDynamicObject::Section ELFDynamicObject::init_array_section() const
{
    return Section(*this, m_init_array_offset, m_init_array_size, sizeof(void (*)()), "DT_INIT_ARRAY");
}

const ELFDynamicObject::Section ELFDynamicObject::fini_array_section() const
{
    return Section(*this, m_fini_array_offset, m_fini_array_size, sizeof(void (*)()), "DT_FINI_ARRAY");
}

const ELFDynamicObject::HashSection ELFDynamicObject::hash_section() const
{
    return HashSection(Section(*this, m_hash_table_offset, 0, 0, "DT_HASH"), HashType::SYSV);
}

const ELFDynamicObject::RelocationSection ELFDynamicObject::relocation_section() const
{
    return RelocationSection(Section(*this, m_relocation_table_offset, m_size_of_relocation_table, m_size_of_relocation_entry, "DT_REL"));
}

const ELFDynamicObject::RelocationSection ELFDynamicObject::plt_relocation_section() const
{
    return RelocationSection(Section(*this, m_plt_relocation_offset_location, m_size_of_plt_relocation_entry_list, m_size_of_relocation_entry, "DT_JMPREL"));
}

u32 ELFDynamicObject::HashSection::calculate_elf_hash(const char* name) const
{
    // SYSV ELF hash algorithm
    // Note that the GNU HASH algorithm has less collisions

    uint32_t hash = 0;
    uint32_t top_nibble_of_hash = 0;

    while (*name != '\0') {
        hash = hash << 4;
        hash += *name;
        name++;

        top_nibble_of_hash = hash & 0xF0000000U;
        if (top_nibble_of_hash != 0)
            hash ^= top_nibble_of_hash >> 24;
        hash &= ~top_nibble_of_hash;
    }

    return hash;
}

u32 ELFDynamicObject::HashSection::calculate_gnu_hash(const char*) const
{
    // FIXME: Implement the GNU hash algorithm
    ASSERT_NOT_REACHED();
}

const ELFDynamicObject::Symbol ELFDynamicObject::HashSection::lookup_symbol(const char* name) const
{
    // FIXME: If we enable gnu hash in the compiler, we should use that here instead
    //     The algo is way better with less collisions
    u32 hash_value = (this->*(m_hash_function))(name);

    u32* hash_table_begin = (u32*)address().as_ptr();

    size_t num_buckets = hash_table_begin[0];

    // This is here for completeness, but, since we're using the fact that every chain
    // will end at chain 0 (which means 'not found'), we don't need to check num_chains.
    // Interestingly, num_chains is required to be num_symbols
    //size_t num_chains = hash_table_begin[1];

    u32* buckets = &hash_table_begin[2];
    u32* chains = &buckets[num_buckets];

    for (u32 i = buckets[hash_value % num_buckets]; i; i = chains[i]) {
        auto symbol = m_dynamic.symbol(i);
        if (strcmp(name, symbol.name()) == 0) {
#ifdef DYNAMIC_LOAD_DEBUG
            dbgprintf("Returning dynamic symbol with index %d for %s: %p\n", i, symbol.name(), symbol.address());
#endif
            return symbol;
        }
    }
    return m_dynamic.the_undefined_symbol();
}

const char* ELFDynamicObject::symbol_string_table_string(Elf32_Word index) const
{
    return (const char*)base_address().offset(m_string_table_offset + index).as_ptr();
}

static const char* name_for_dtag(Elf32_Sword d_tag)
{
    switch (d_tag) {
    case DT_NULL:
        return "NULL"; /* marks end of _DYNAMIC array */
    case DT_NEEDED:
        return "NEEDED"; /* string table offset of needed lib */
    case DT_PLTRELSZ:
        return "PLTRELSZ"; /* size of relocation entries in PLT */
    case DT_PLTGOT:
        return "PLTGOT"; /* address PLT/GOT */
    case DT_HASH:
        return "HASH"; /* address of symbol hash table */
    case DT_STRTAB:
        return "STRTAB"; /* address of string table */
    case DT_SYMTAB:
        return "SYMTAB"; /* address of symbol table */
    case DT_RELA:
        return "RELA"; /* address of relocation table */
    case DT_RELASZ:
        return "RELASZ"; /* size of relocation table */
    case DT_RELAENT:
        return "RELAENT"; /* size of relocation entry */
    case DT_STRSZ:
        return "STRSZ"; /* size of string table */
    case DT_SYMENT:
        return "SYMENT"; /* size of symbol table entry */
    case DT_INIT:
        return "INIT"; /* address of initialization func. */
    case DT_FINI:
        return "FINI"; /* address of termination function */
    case DT_SONAME:
        return "SONAME"; /* string table offset of shared obj */
    case DT_RPATH:
        return "RPATH"; /* string table offset of library search path */
    case DT_SYMBOLIC:
        return "SYMBOLIC"; /* start sym search in shared obj. */
    case DT_REL:
        return "REL"; /* address of rel. tbl. w addends */
    case DT_RELSZ:
        return "RELSZ"; /* size of DT_REL relocation table */
    case DT_RELENT:
        return "RELENT"; /* size of DT_REL relocation entry */
    case DT_PLTREL:
        return "PLTREL"; /* PLT referenced relocation entry */
    case DT_DEBUG:
        return "DEBUG"; /* bugger */
    case DT_TEXTREL:
        return "TEXTREL"; /* Allow rel. mod. to unwritable seg */
    case DT_JMPREL:
        return "JMPREL"; /* add. of PLT's relocation entries */
    case DT_BIND_NOW:
        return "BIND_NOW"; /* Bind now regardless of env setting */
    case DT_INIT_ARRAY:
        return "INIT_ARRAY"; /* address of array of init func */
    case DT_FINI_ARRAY:
        return "FINI_ARRAY"; /* address of array of term func */
    case DT_INIT_ARRAYSZ:
        return "INIT_ARRAYSZ"; /* size of array of init func */
    case DT_FINI_ARRAYSZ:
        return "FINI_ARRAYSZ"; /* size of array of term func */
    case DT_RUNPATH:
        return "RUNPATH"; /* strtab offset of lib search path */
    case DT_FLAGS:
        return "FLAGS"; /* Set of DF_* flags */
    case DT_ENCODING:
        return "ENCODING"; /* further DT_* follow encoding rules */
    case DT_PREINIT_ARRAY:
        return "PREINIT_ARRAY"; /* address of array of preinit func */
    case DT_PREINIT_ARRAYSZ:
        return "PREINIT_ARRAYSZ"; /* size of array of preinit func */
    case DT_LOOS:
        return "LOOS"; /* reserved range for OS */
    case DT_HIOS:
        return "HIOS"; /*  specific dynamic array tags */
    case DT_LOPROC:
        return "LOPROC"; /* reserved range for processor */
    case DT_HIPROC:
        return "HIPROC"; /*  specific dynamic array tags */
    case DT_GNU_HASH:
        return "GNU_HASH"; /* address of GNU hash table */
    case DT_RELACOUNT:
        return "RELACOUNT"; /* if present, number of RELATIVE */
    case DT_RELCOUNT:
        return "RELCOUNT"; /* relocs, which must come first */
    case DT_FLAGS_1:
        return "FLAGS_1";
    default:
        return "??";
    }
}
