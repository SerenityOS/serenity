#include <AK/StringBuilder.h>
#include <LibELF/ELFDynamicLoader.h>

#include <assert.h>
#include <dlfcn.h>
#include <mman.h>
#include <stdio.h>
#include <stdlib.h>

#define DYNAMIC_LOAD_DEBUG
//#define DYNAMIC_LOAD_VERBOSE

#ifdef DYNAMIC_LOAD_VERBOSE
#    define VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)
#else
#    define VERBOSE(fmt, ...) \
        do {                  \
        } while (0)
#endif

static bool s_always_bind_now = false;

NonnullRefPtr<ELFDynamicLoader> ELFDynamicLoader::construct(const char* filename, int fd, size_t size)
{
    return adopt(*new ELFDynamicLoader(filename, fd, size));
}

ELFDynamicLoader::ELFDynamicLoader(const char* filename, int fd, size_t size)
    : m_filename(filename)
    , m_file_size(size)
    , m_image_fd(fd)
{
    String file_mmap_name = String::format("ELF_DYN: %s", m_filename.characters());

    // FIXME: When MAP_PRIVATE is implemented for file-backed regions, change to MAP_PRIVATE
    m_file_mapping = mmap_with_name(nullptr, size, PROT_READ, MAP_SHARED, m_image_fd, 0, file_mmap_name.characters());
    if (MAP_FAILED == m_file_mapping) {
        m_valid = false;
    }
}

ELFDynamicLoader::~ELFDynamicLoader()
{
    if (MAP_FAILED != m_file_mapping)
        munmap(m_file_mapping, m_file_size);
}

void* ELFDynamicLoader::symbol_for_name(const char* name)
{
    auto symbol = m_dynamic_object->hash_section().lookup_symbol(name);

    if (symbol.is_undefined())
        return nullptr;

    return m_dynamic_object->base_address().offset(symbol.value()).as_ptr();
}

bool ELFDynamicLoader::load_from_image(unsigned flags)
{
    ELFImage elf_image((u8*)m_file_mapping, m_file_size);

    m_valid = elf_image.is_valid() && elf_image.is_dynamic();

    if (!m_valid) {
        return false;
    }

#ifdef DYNAMIC_LOAD_VERBOSE
    m_image->dump();
#endif

    load_program_headers(elf_image);

    m_dynamic_object = AK::make<ELFDynamicObject>(m_text_segment_load_address, m_dynamic_section_address);

    return load_stage_2(flags);
}

bool ELFDynamicLoader::load_stage_2(unsigned flags)
{
    ASSERT(flags & RTLD_GLOBAL);
    ASSERT(flags & RTLD_LAZY);

#ifdef DYNAMIC_LOAD_DEBUG
    m_dynamic_object->dump();
#endif

    if (m_dynamic_object->has_text_relocations()) {
        ASSERT(m_text_segment_load_address.get() != 0);
        if (0 > mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_WRITE)) {
            perror("mprotect .text: PROT_READ | PROT_WRITE"); // FIXME: dlerror?
            return false;
        }
    }

    do_relocations();
    setup_plt_trampoline();

    // Clean up our setting of .text to PROT_READ | PROT_WRITE
    if (m_dynamic_object->has_text_relocations()) {
        if (0 > mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_EXEC)) {
            perror("mprotect .text: PROT_READ | PROT_EXEC"); // FIXME: dlerror?
            return false;
        }
    }

    call_object_init_functions();

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Loaded %s\n", m_filename.characters());
#endif
    return true;
}

void ELFDynamicLoader::load_program_headers(const ELFImage& elf_image)
{
    size_t total_required_allocation_size = 0; // NOTE: If we don't have any TEXTREL, we can keep RO data RO, which would be nice

    Vector<ProgramHeaderRegion> program_headers;

    ProgramHeaderRegion* text_region_ptr = nullptr;
    ProgramHeaderRegion* data_region_ptr = nullptr;
    ProgramHeaderRegion* tls_region_ptr = nullptr;
    VirtualAddress dynamic_region_desired_vaddr;

    elf_image.for_each_program_header([&](const ELFImage::ProgramHeader& program_header) {
        ProgramHeaderRegion new_region;
        new_region.set_program_header(program_header.raw_header());
        if (new_region.is_load())
            total_required_allocation_size += new_region.required_load_size();
        program_headers.append(move(new_region));
        auto& region = program_headers.last();
        if (region.is_tls_template())
            tls_region_ptr = &region;
        else if (region.is_load()) {
            if (region.is_executable())
                text_region_ptr = &region;
            else
                data_region_ptr = &region;
        }
        else if (region.is_dynamic()) {
            dynamic_region_desired_vaddr = region.desired_load_address();
        }
    });

    ASSERT(text_region_ptr && data_region_ptr);

    // Process regions in order: .text, .data, .tls
    auto* region = text_region_ptr;
    // FIXME: When MAP_PRIVATE is implemented for file-backed regions, change to MAP_PRIVATE without the mprotect and memcpy
    //void* text_segment_begin = mmap_with_name(nullptr, region->required_load_size(), region->mmap_prot(), MAP_PRIVATE, m_image_fd, region->offset(), String::format(".text: %s", m_filename.characters()).characters());
    void* text_segment_begin = mmap_with_name(nullptr, region->required_load_size(), PROT_READ | PROT_WRITE , MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, String::format(".text: %s", m_filename.characters()).characters());
    if (MAP_FAILED == text_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    m_text_segment_size = region->required_load_size();
    m_text_segment_load_address = VirtualAddress { (u32)text_segment_begin };
    memcpy(m_text_segment_load_address.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());
    if (0 > mprotect(text_segment_begin, m_text_segment_size, region->mmap_prot())) {
        perror("mprotect .text PROT_READ | PROT_EXEC");
        ASSERT_NOT_REACHED();
    }

    m_dynamic_section_address = dynamic_region_desired_vaddr.offset(m_text_segment_load_address.get());

    region = data_region_ptr;
    void* data_segment_begin = mmap_with_name((u8*)text_segment_begin + m_text_segment_size, region->required_load_size(), region->mmap_prot(), MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, String::format(".data: %s", m_filename.characters()).characters());
    if (MAP_FAILED == data_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    VirtualAddress data_segment_actual_addr = region->desired_load_address().offset((u32)text_segment_begin);
    memcpy(data_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());

    // FIXME: Do some kind of 'allocate TLS section' or some such from a per-application pool
    if (tls_region_ptr) {
        region = tls_region_ptr;
        // FIXME: This can't be right either. TLS needs some real work i'd say :)
        m_tls_segment_address = tls_region_ptr->desired_load_address();
        VirtualAddress tls_segment_actual_addr = region->desired_load_address().offset((u32)text_segment_begin);
        memcpy(tls_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());
    }
}

void ELFDynamicLoader::do_relocations()
{
    u32 load_base_address = m_dynamic_object->base_address().get();

    // FIXME: We should really bail on undefined symbols here.

    auto main_relocation_section = m_dynamic_object->relocation_section();

    main_relocation_section.for_each_relocation([&](const ELFDynamicObject::Relocation& relocation) {
        VERBOSE("====== RELOCATION %d: offset 0x%08X, type %d, symidx %08X\n", relocation.offset_in_section() / main_relocation_section.entry_size(), relocation.offset(), relocation.type(), relocation.symbol_index());
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
            u32 symbol_address = symbol.value() + load_base_address;
            *patch_ptr += symbol_address;
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
            u32 symbol_location = load_base_address + symbol.value();
            *patch_ptr = symbol_location;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_RELATIVE: {
            // FIXME: According to the spec, R_386_relative ones must be done first.
            //     We could explicitly do them first using m_number_of_relocatoins from DT_RELCOUNT
            //     However, our compiler is nice enough to put them at the front of the relocations for us :)
            VERBOSE("Load address relocation at offset %X\n", relocation.offset());
            VERBOSE("    patch ptr == %p, adding load base address (%p) to it and storing %p\n", *patch_ptr, load_base_address, *patch_ptr + load_base_address);
            *patch_ptr += load_base_address; // + addend for RelA (addend for Rel is stored at addr)
            break;
        }
        case R_386_TLS_TPOFF: {
            VERBOSE("Relocation type: R_386_TLS_TPOFF at offset %X\n", relocation.offset());
            // FIXME: this can't be right? I have no idea what "negative offset into TLS storage" means...
            // FIXME: Check m_has_static_tls and do something different for dynamic TLS
            *patch_ptr = relocation.offset() - (u32)m_tls_segment_address.as_ptr() - *patch_ptr;
            break;
        }
        default:
            // Raise the alarm! Someone needs to implement this relocation type
            dbgprintf("Found a new exciting relocation type %d\n", relocation.type());
            printf("ELFDynamicLoader: Found unknown relocation type %d\n", relocation.type());
            ASSERT_NOT_REACHED();
            break;
        }
        return IterationDecision::Continue;
    });

    // Handle PLT Global offset table relocations.
    m_dynamic_object->plt_relocation_section().for_each_relocation([&](const ELFDynamicObject::Relocation& relocation) {
        // FIXME: Or BIND_NOW flag passed in?
        if (m_dynamic_object->must_bind_now() || s_always_bind_now) {
            // Eagerly BIND_NOW the PLT entries, doing all the symbol looking goodness
            // The patch method returns the address for the LAZY fixup path, but we don't need it here
            (void)patch_plt_entry(relocation.offset_in_section());
        } else {
            // LAZY-ily bind the PLT slots by just adding the base address to the offsets stored there
            // This avoids doing symbol lookup, which might be expensive
            ASSERT(relocation.type() == R_386_JMP_SLOT);

            u8* relocation_address = relocation.address().as_ptr();

            *(u32*)relocation_address += load_base_address;
        }
        return IterationDecision::Continue;
    });

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Done relocating!\n");
#endif
}

// Defined in <arch>/plt_trampoline.S
extern "C" void _plt_trampoline(void) __attribute__((visibility("hidden")));

void ELFDynamicLoader::setup_plt_trampoline()
{
    VirtualAddress got_address = m_dynamic_object->plt_got_base_address();

    u32* got_u32_ptr = (u32*)got_address.as_ptr();
    got_u32_ptr[1] = (u32)this;
    got_u32_ptr[2] = (u32)&_plt_trampoline;

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Set GOT PLT entries at %p: [0] = %p [1] = %p, [2] = %p\n", got_u32_ptr, got_u32_ptr[0], got_u32_ptr[1], got_u32_ptr[2]);
#endif
}

// Called from our ASM routine _plt_trampoline
extern "C" Elf32_Addr _fixup_plt_entry(ELFDynamicLoader* object, u32 relocation_offset)
{
    return object->patch_plt_entry(relocation_offset);
}

// offset is in PLT relocation table
Elf32_Addr ELFDynamicLoader::patch_plt_entry(u32 relocation_offset)
{
    auto relocation = m_dynamic_object->plt_relocation_section().relocation_at_offset(relocation_offset);

    ASSERT(relocation.type() == R_386_JMP_SLOT);

    auto sym = relocation.symbol();

    u8* relocation_address = relocation.address().as_ptr();
    u32 symbol_location = sym.address().get();

    VERBOSE("ELFDynamicLoader: Jump slot relocation: putting %s (%p) into PLT at %p\n", sym.name(), symbol_location, relocation_address);

    *(u32*)relocation_address = symbol_location;

    return symbol_location;
}

void ELFDynamicLoader::call_object_init_functions()
{
    typedef void (*InitFunc)();
    auto init_function = (InitFunc)(m_dynamic_object->init_section().address().as_ptr());

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Calling DT_INIT at %p\n", init_function);
#endif
    (init_function)();

    auto init_array_section = m_dynamic_object->init_array_section();

    InitFunc* init_begin = (InitFunc*)(init_array_section.address().as_ptr());
    InitFunc* init_end = init_begin + init_array_section.entry_count();
    while (init_begin != init_end) {
        // Android sources claim that these can be -1, to be ignored.
        // 0 definitely shows up. Apparently 0/-1 are valid? Confusing.
        if (!*init_begin || ((i32)*init_begin == -1))
            continue;
#ifdef DYNAMIC_LOAD_DEBUG
        dbgprintf("Calling DT_INITARRAY entry at %p\n", *init_begin);
#endif
        (*init_begin)();
        ++init_begin;
    }
}

u32 ELFDynamicLoader::ProgramHeaderRegion::mmap_prot() const
{
    int prot = 0;
    prot |= is_executable() ? PROT_EXEC : 0;
    prot |= is_readable() ? PROT_READ : 0;
    prot |= is_writable() ? PROT_WRITE : 0;
    return prot;
}
