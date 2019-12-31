#include <AK/StringBuilder.h>
#include <LibELF/ELFDynamicObject.h>

#include <assert.h>
#include <mman.h>
#include <stdio.h>
#include <stdlib.h>

#define DYNAMIC_LOAD_DEBUG
//#define DYNAMIC_LOAD_VERBOSE

#ifdef DYNAMIC_LOAD_VERBOSE
#    define VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)
#else
#    define VERBOSE(fmt, ...) do { } while (0)
#endif

static bool s_always_bind_now = true;

static const char* name_for_dtag(Elf32_Sword tag);

// SYSV ELF hash algorithm
// Note that the GNU HASH algorithm has less collisions
static uint32_t calculate_elf_hash(const char* name)
{
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

NonnullRefPtr<ELFDynamicObject> ELFDynamicObject::construct(const char* filename, int fd, size_t size)
{
    return adopt(*new ELFDynamicObject(filename, fd, size));
}

ELFDynamicObject::ELFDynamicObject(const char* filename, int fd, size_t size)
    : m_filename(filename)
    , m_file_size(size)
    , m_image_fd(fd)
{
    String file_mmap_name = String::format("ELF_DYN: %s", m_filename.characters());

    m_file_mapping = mmap_with_name(nullptr, size, PROT_READ, MAP_PRIVATE, m_image_fd, 0, file_mmap_name.characters());
    if (MAP_FAILED == m_file_mapping) {
        m_valid = false;
        return;
    }

    m_image = AK::make<ELFImage>((u8*)m_file_mapping);

    m_valid = m_image->is_valid() && m_image->parse() && m_image->is_dynamic();

    if (!m_valid) {
        return;
    }

    const ELFImage::DynamicSection probably_dynamic_section = m_image->dynamic_section();
    if (StringView(".dynamic") != probably_dynamic_section.name() || probably_dynamic_section.type() != SHT_DYNAMIC) {
        m_valid = false;
        return;
    }
}

ELFDynamicObject::~ELFDynamicObject()
{
    if (MAP_FAILED != m_file_mapping)
        munmap(m_file_mapping, m_file_size);
}

void ELFDynamicObject::dump()
{
    auto dynamic_section = m_image->dynamic_section();

    StringBuilder builder;
    builder.append("\nd_tag      tag_name         value\n");
    size_t num_dynamic_sections = 0;

    dynamic_section.for_each_dynamic_entry([&](const ELFImage::DynamicSectionEntry& entry) {
        String name_field = String::format("(%s)", name_for_dtag(entry.tag()));
        builder.appendf("0x%08X %-17s0x%X\n", entry.tag(), name_field.characters(), entry.val());
        num_dynamic_sections++;
        return IterationDecision::Continue;
    });

    dbgprintf("Dynamic section at offset 0x%x contains %zu entries:\n", dynamic_section.offset(), num_dynamic_sections);
    dbgprintf(builder.to_string().characters());
}

void ELFDynamicObject::parse_dynamic_section()
{
    auto dynamic_section = m_image->dynamic_section();
    dynamic_section.for_each_dynamic_entry([&](const ELFImage::DynamicSectionEntry& entry) {
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
            m_must_bind_now = entry.val() & DF_BIND_NOW;
            m_has_text_relocations = entry.val() & DF_TEXTREL;
            m_should_process_origin = entry.val() & DF_ORIGIN;
            m_has_static_thread_local_storage = entry.val() & DF_STATIC_TLS;
            m_requires_symbolic_symbol_resolution = entry.val() & DF_SYMBOLIC;
            break;
        case DT_TEXTREL:
            m_has_text_relocations = true; // This tag seems to exist for legacy reasons only?
            break;
        default:
            dbgprintf("ELFDynamicObject: DYNAMIC tag handling not implemented for DT_%s\n", name_for_dtag(entry.tag()));
            printf("ELFDynamicObject: DYNAMIC tag handling not implemented for DT_%s\n", name_for_dtag(entry.tag()));
            ASSERT_NOT_REACHED(); // FIXME: Maybe just break out here and return false?
            break;
        }
        return IterationDecision::Continue;
    });
}

typedef void (*InitFunc)();

bool ELFDynamicObject::load(unsigned flags)
{
    ASSERT(flags & RTLD_GLOBAL);
    ASSERT(flags & RTLD_LAZY);

#ifdef DYNAMIC_LOAD_DEBUG
    dump();
#endif
#ifdef DYNAMIC_LOAD_VERBOSE
    m_image->dump();
#endif

    parse_dynamic_section();

    // FIXME: be more flexible?
    size_t total_required_allocation_size = 0;

    // FIXME: Can we re-use ELFLoader? This and what follows looks a lot like what's in there...
    //     With the exception of using desired_load_address().offset(text_segment_begin)
    //     It seems kinda gross to expect the program headers to be in a specific order..
    m_image->for_each_program_header([&](const ELFImage::ProgramHeader& program_header) {
        ProgramHeaderRegion new_region(program_header.raw_header());
        if (new_region.is_load())
            total_required_allocation_size += new_region.required_load_size();
        m_program_header_regions.append(move(new_region));
        auto& region = m_program_header_regions.last();
        if (region.is_tls_template())
            m_tls_region = &region;
        else if (region.is_load()) {
            if (region.is_executable())
                m_text_region = &region;
            else
                m_data_region = &region;
        }
    });

    ASSERT(m_text_region && m_data_region);

    // Process regions in order: .text, .data, .tls
    auto* region = m_text_region;
    void* text_segment_begin = mmap_with_name(nullptr, region->required_load_size(), region->mmap_prot(), MAP_PRIVATE, m_image_fd, region->offset(), String::format(".text: %s", m_filename.characters()).characters());
    size_t text_segment_size = region->required_load_size();
    region->set_base_address(VirtualAddress { (u32)text_segment_begin });
    region->set_load_address(VirtualAddress { (u32)text_segment_begin });

    region = m_data_region;
    void* data_segment_begin = mmap_with_name((u8*)text_segment_begin + text_segment_size, region->required_load_size(), region->mmap_prot(), MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, String::format(".data: %s", m_filename.characters()).characters());
    size_t data_segment_size = region->required_load_size();
    VirtualAddress data_segment_actual_addr = region->desired_load_address().offset((u32)text_segment_begin);
    region->set_base_address(VirtualAddress { (u32)text_segment_begin });
    region->set_load_address(data_segment_actual_addr);
    memcpy(data_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());

    if (m_tls_region) {
        region = m_data_region;
        VirtualAddress tls_segment_actual_addr = region->desired_load_address().offset((u32)text_segment_begin);
        region->set_base_address(VirtualAddress { (u32)text_segment_begin });
        region->set_load_address(tls_segment_actual_addr);
        memcpy(tls_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());
    }

    // sanity check
    u8* end_of_in_memory_image = (u8*)data_segment_begin + data_segment_size;
    ASSERT((ptrdiff_t)total_required_allocation_size == (ptrdiff_t)(end_of_in_memory_image - (u8*)text_segment_begin));

    if (m_has_text_relocations) {
        if (0 > mprotect(m_text_region->load_address().as_ptr(), m_text_region->required_load_size(), PROT_READ | PROT_WRITE)) {
            perror("mprotect"); // FIXME: dlerror?
            return false;
        }
    }

    do_relocations();

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Done relocating!\n");
#endif

    // FIXME: PLT patching doesn't seem to work as expected.
    //     Need to dig into the spec to see what we're doing wrong
    //     Hopefully it won't need an assembly entry point... :/
    ///    For now we can just BIND_NOW every time

    // This should be the address of section ".got.plt"
    const ELFImage::Section& got_section = m_image->lookup_section(".got.plt");
    VirtualAddress got_address = m_text_region->load_address().offset(got_section.address());

    u32* got_u32_ptr = reinterpret_cast<u32*>(got_address.as_ptr());
    got_u32_ptr[1] = (u32)this;
    got_u32_ptr[2] = (u32)&ELFDynamicObject::patch_plt_entry;

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Set GOT PLT entries at %p: [0] = %p [1] = %p, [2] = %p\n", got_u32_ptr, got_u32_ptr[0], got_u32_ptr[1], got_u32_ptr[2]);
#endif

    // Clean up our setting of .text to PROT_READ | PROT_WRITE
    if (m_has_text_relocations) {
        if (0 > mprotect(m_text_region->load_address().as_ptr(), m_text_region->required_load_size(), PROT_READ | PROT_EXEC)) {
            perror("mprotect"); // FIXME: dlerror?
            return false;
        }
    }

    u8* load_addr = m_text_region->load_address().as_ptr();
    InitFunc init_function = (InitFunc)(load_addr + m_init_offset);

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Calling DT_INIT at %p\n", init_function);
#endif
    // FIXME:
    // Disassembly of section .init:
    //
    //  00007e98 <_init>:
    //        7e98:       55                      push   ebp
    //
    // Where da ret at? related to -nostartfiles for sure...
    //(init_function)();

    InitFunc* init_begin = (InitFunc*)(load_addr + m_init_array_offset);
    u32 init_end = (u32)((u8*)init_begin + m_init_array_size);
    while ((u32)init_begin < init_end) {
        // Andriod sources claim that these can be -1, to be ignored.
        // 0 definitely shows up. Apparently 0/-1 are valid? Confusing.
        if (!*init_begin || ((i32)*init_begin == -1))
            continue;
#ifdef DYNAMIC_LOAD_DEBUG
        dbgprintf("Calling DT_INITARRAY entry at %p\n", *init_begin);
#endif
        (*init_begin)();
        ++init_begin;
    }

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Loaded %s\n", m_filename.characters());
#endif
    // FIXME: return false sometimes? missing symbol etc
    return true;
}

void* ELFDynamicObject::symbol_for_name(const char* name)
{
    // FIXME: If we enable gnu hash in the compiler, we should use that here instead
    //     The algo is way better with less collisions
    uint32_t hash_value = calculate_elf_hash(name);

    u8* load_addr = m_text_region->load_address().as_ptr();

    // NOTE: We need to use the loaded hash/string/symbol tables here to get the right
    //    addresses. The ones that are in the ELFImage won't cut it, they aren't relocated
    u32* hash_table_begin = (u32*)(load_addr + m_hash_table_offset);
    Elf32_Sym* symtab = (Elf32_Sym*)(load_addr + m_symbol_table_offset);
    const char* strtab = (const char*)load_addr + m_string_table_offset;

    size_t num_buckets = hash_table_begin[0];

    // This is here for completeness, but, since we're using the fact that every chain
    // will end at chain 0 (which means 'not found'), we don't need to check num_chains.
    // Interestingly, num_chains is required to be num_symbols
    //size_t num_chains = hash_table_begin[1];

    u32* buckets = &hash_table_begin[2];
    u32* chains = &buckets[num_buckets];

    for (u32 i = buckets[hash_value % num_buckets]; i; i = chains[i]) {
        if (strcmp(name, strtab + symtab[i].st_name) == 0) {
            void* retval = load_addr + symtab[i].st_value;
#ifdef DYNAMIC_LOAD_DEBUG
            dbgprintf("Returning dynamic symbol with index %d for %s: %p\n", i, strtab + symtab[i].st_name, retval);
#endif
            return retval;
        }
    }

    return nullptr;
}

// offset is from PLT entry
// Tag is inserted into GOT #2 for 'this' DSO (literally the this pointer)
void ELFDynamicObject::patch_plt_entry(u32 got_offset, void* dso_got_tag)
{
    // FIXME: This is never called :(
    CRASH();
    dbgprintf("------ PATCHING PLT ENTRY -------");
    // NOTE: We put 'this' into the GOT when we loaded it into memory
    auto* dynamic_object_object = reinterpret_cast<ELFDynamicObject*>(dso_got_tag);

    // FIXME: might actually be a RelA, check m_plt_relocation_type
    // u32 base_addr_offset = dynamic_object_object->m_relocation_table_offset + got_offset;
    // Elf32_Rel relocation = *reinterpret_cast<Elf32_Rel*>(&((u8*)dynamic_object_object->m_file_mapping)[base_addr_offset]);
    u32 relocation_index = got_offset / dynamic_object_object->m_size_of_relocation_entry;
    auto relocation = dynamic_object_object->m_image->dynamic_relocation_section().relocation(relocation_index);

    ASSERT(relocation.type() == R_386_JMP_SLOT);

    auto sym = relocation.symbol();

    auto* text_load_address = dynamic_object_object->m_text_region->load_address().as_ptr();
    u8* relocation_address = text_load_address + relocation.offset();

    if (0 > mprotect(text_load_address, dynamic_object_object->m_text_region->required_load_size(), PROT_READ | PROT_WRITE)) {
        ASSERT_NOT_REACHED(); // uh oh, no can do boss
    }

    dbgprintf("Found relocation address: %p for %s", relocation_address, sym.name());

    *(u32*)relocation_address = (u32)(text_load_address + sym.value());

    if (0 > mprotect(text_load_address, dynamic_object_object->m_text_region->required_load_size(), PROT_READ | PROT_EXEC)) {
        ASSERT_NOT_REACHED(); // uh oh, no can do boss
    }

    CRASH();
    // FIXME: Call the relocated method here?
}

void ELFDynamicObject::do_relocations()
{
    auto dyn_relocation_section = m_image->dynamic_relocation_section();
    if (StringView(".rel.dyn") != dyn_relocation_section.name() || SHT_REL != dyn_relocation_section.type()) {
        ASSERT_NOT_REACHED();
    }

    u8* load_base_address = m_text_region->base_address().as_ptr();

    int i = -1;

    // FIXME: We should really bail on undefined symbols here. (but, there's some TLS vars that are currently undef soooo.... :) )

    dyn_relocation_section.for_each_relocation([&](const ELFImage::DynamicRelocation& relocation) {
        ++i;
        VERBOSE("====== RELOCATION %d: offset 0x%08X, type %d, symidx %08X\n", i, relocation.offset(), relocation.type(), relocation.symbol_index());
        u32* patch_ptr = (u32*)(load_base_address + relocation.offset());
        switch (relocation.type()) {
        case R_386_NONE:
            // Apparently most loaders will just skip these?
            // Seems if the 'link editor' generates one something is funky with your code
            VERBOSE("None relocation. No symbol, no nothin.\n");
            break;
        case R_386_32: {
            auto symbol = relocation.symbol();

            VERBOSE("Absolute relocation: name: '%s', value: %p\n", symbol.name(), symbol.value());
            if (symbol.bind() == STB_LOCAL) {
                u32 symbol_address = symbol.section().address() + symbol.value();
                *patch_ptr += symbol_address;
            } else if (symbol.bind() == STB_GLOBAL) {
                u32 symbol_address = symbol.value() + (u32)load_base_address;
                *patch_ptr += symbol_address;
            } else if (symbol.bind() == STB_WEAK) {
                // FIXME: Handle weak symbols...
                dbgprintf("ELFDynamicObject: Ignoring weak symbol %s\n", symbol.name());
            } else {
                VERBOSE("Found new fun symbol bind value %d\n", symbol.bind());
                ASSERT_NOT_REACHED();
            }
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_PC32: {
            auto symbol = relocation.symbol();
            VERBOSE("PC-relative relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            u32 relative_offset = (symbol.value() - relocation.offset());
            *patch_ptr += relative_offset;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_GLOB_DAT: {
            auto symbol = relocation.symbol();
            VERBOSE("Global data relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            u32 symbol_location = (u32)(m_data_region->base_address().as_ptr() + symbol.value());
            *patch_ptr = symbol_location;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_RELATIVE: {
            // FIXME: According to the spec, R_386_relative ones must be done first.
            //     We could explicitly do them first using m_number_of_relocatoins from DT_RELCOUNT
            //     However, our compiler is nice enough to put them at the front of the relocations for us :)
            VERBOSE("Load address relocation at offset %X\n", relocation.offset());
            VERBOSE("    patch ptr == %p, adding load base address (%p) to it and storing %p\n", *patch_ptr, load_base_address, *patch_ptr + (u32)load_base_address);
            *patch_ptr += (u32)load_base_address; // + addend for RelA (addend for Rel is stored at addr)
            break;
        }
        case R_386_TLS_TPOFF: {
            VERBOSE("Relocation type: R_386_TLS_TPOFF at offset %X\n", relocation.offset());
            // FIXME: this can't be right? I have no idea what "negative offset into TLS storage" means...
            // FIXME: Check m_has_static_tls and do something different for dynamic TLS
            VirtualAddress tls_region_loctation = m_tls_region->desired_load_address();
            *patch_ptr = relocation.offset() - (u32)tls_region_loctation.as_ptr() - *patch_ptr;
            break;
        }
        default:
            // Raise the alarm! Someone needs to implement this relocation type
            dbgprintf("Found a new exciting relocation type %d\n", relocation.type());
            printf("ELFDynamicObject: Found unknown relocation type %d\n", relocation.type());
            ASSERT_NOT_REACHED();
            break;
        }
        return IterationDecision::Continue;
    });

    // FIXME: Or BIND_NOW flag passed in?
    if (m_must_bind_now || s_always_bind_now) {
        // FIXME: Why do we keep jumping to the entry in the GOT without going to our callback first?
        //     that would make this s_always_bind_now redundant

        for (size_t idx = 0; idx < m_size_of_plt_relocation_entry_list; idx += m_size_of_relocation_entry) {
            VirtualAddress relocation_vaddr = m_text_region->load_address().offset(m_plt_relocation_offset_location).offset(idx);
            Elf32_Rel* jump_slot_relocation = (Elf32_Rel*)relocation_vaddr.as_ptr();

            ASSERT(ELF32_R_TYPE(jump_slot_relocation->r_info) == R_386_JMP_SLOT);

            auto sym = m_image->dynamic_symbol(ELF32_R_SYM(jump_slot_relocation->r_info));

            auto* image_base_address = m_text_region->base_address().as_ptr();
            u8* relocation_address = image_base_address + jump_slot_relocation->r_offset;
            u32 symbol_location = (u32)(image_base_address + sym.value());

            VERBOSE("ELFDynamicObject: Jump slot relocation: putting %s (%p) into PLT at %p\n", sym.name(), symbol_location, relocation_address);

            *(u32*)relocation_address = symbol_location;
        }
    }
}

u32 ELFDynamicObject::ProgramHeaderRegion::mmap_prot() const
{
    int prot = 0;
    prot |= is_executable() ? PROT_EXEC : 0;
    prot |= is_readable() ? PROT_READ : 0;
    prot |= is_writable() ? PROT_WRITE : 0;
    return prot;
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
