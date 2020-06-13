/*
 * Copyright (c) 2019-2020, Andrew Kaster <andrewdkaster@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/Validation.h>

#include <assert.h>
#include <dlfcn.h>
#include <mman.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define DYNAMIC_LOAD_DEBUG
#define DYNAMIC_LOAD_VERBOSE

#ifdef DYNAMIC_LOAD_VERBOSE
#    define VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)
#else
#    define VERBOSE(fmt, ...) \
        do {                  \
        } while (0)
#endif

namespace ELF {

static bool s_always_bind_now = false;

NonnullRefPtr<DynamicLoader> DynamicLoader::construct(const char* filename, int fd, size_t size)
{
    return adopt(*new DynamicLoader(filename, fd, size));
}

DynamicLoader::DynamicLoader(const char* filename, int fd, size_t size)
    : m_filename(filename)
    , m_file_size(size)
    , m_image_fd(fd)
{
    if (m_file_size < sizeof(Elf32_Ehdr)) {
        m_valid = false;
        return;
    }

    String file_mmap_name = String::format("ELF_DYN: %s", m_filename.characters());

    m_file_mapping = mmap_with_name(nullptr, m_file_size, PROT_READ, MAP_PRIVATE, m_image_fd, 0, file_mmap_name.characters());
    if (MAP_FAILED == m_file_mapping) {
        m_valid = false;
        return;
    }

    auto* elf_header = (Elf32_Ehdr*)m_file_mapping;

    if (!validate_elf_header(*elf_header, m_file_size) || !validate_program_headers(*elf_header, m_file_size, (u8*)m_file_mapping, m_file_size, m_program_interpreter)) {
        m_valid = false;
    }
}

DynamicLoader::~DynamicLoader()
{
    if (MAP_FAILED != m_file_mapping)
        munmap(m_file_mapping, m_file_size);
}

void* DynamicLoader::symbol_for_name(const char* name) const
{
    dbg() << "symbol_for_name: " << name;
    auto symbol = m_dynamic_object->hash_section().lookup_symbol(name);
    dbg() << "is_undefined? : " << symbol.is_undefined();
    dbg() << "value : " << symbol.value();

    if (symbol.is_undefined())
        return nullptr;

    return m_dynamic_object->base_address().offset(symbol.value()).as_ptr();
}

bool DynamicLoader::load_from_image(unsigned flags)
{
    Image elf_image((u8*)m_file_mapping, m_file_size);

    m_valid = elf_image.is_valid() && elf_image.is_dynamic();

    if (!m_valid) {
        return false;
    }

    // #ifdef DYNAMIC_LOAD_VERBOSE
    //     m_image->dump();
    // #endif

    load_program_headers(elf_image);

    m_entry_point.set(elf_image.entry().get() + m_text_segment_load_address.get());

    // Don't need this private mapping anymore
    munmap(m_file_mapping, m_file_size);
    m_file_mapping = MAP_FAILED;

    m_dynamic_object = AK::make<DynamicObject>(m_text_segment_load_address, m_dynamic_section_address);

    ASSERT(!m_tls_segment_address.as_ptr() || m_dynamic_object->has_static_thread_local_storage());

    m_dynamic_object->for_each_needed_library([](const char* lib_name) {
        dbg() << "Loading dependency: " << lib_name;
        printf("Loading dependency: %s\n", lib_name);
        // FIXME: We shouldn't have /usr/lib hardcoded, and we should iterate over some pre-defined list of directories
        auto res = dlopen(String::format("/usr/lib/%s", lib_name).characters(), RTLD_LAZY | RTLD_GLOBAL);
        if (!res) {
            perror(String::format("Failed to load required library: %s\ndlerr: %s\n", lib_name, dlerror()).characters()); // FIXME: dlerror?
            ASSERT_NOT_REACHED();
        }
        return IterationDecision::Continue;
    });

    return load_stage_2(flags);
}

bool DynamicLoader::load_stage_2(unsigned flags)
{
    ASSERT(flags & RTLD_GLOBAL);
    ASSERT(flags & RTLD_LAZY);

#ifdef DYNAMIC_LOAD_DEBUG
    m_dynamic_object->dump();
#endif

    if (m_dynamic_object->has_text_relocations()) {
        dbg() << "Someone linked non -fPIC code into " << m_filename << " :(";
        ASSERT(m_text_segment_load_address.get() != 0);
        if (0 > mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_WRITE)) {
            perror("mprotect .text: PROT_READ | PROT_WRITE"); // FIXME: dlerror?
            return false;
        }
    }

    do_relocations();
    setup_plt_trampoline();
    // relocate_got_plt();

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

void DynamicLoader::load_program_headers(const Image& elf_image)
{
    Vector<ProgramHeaderRegion> program_headers;

    Optional<ProgramHeaderRegion> text_region;
    Optional<ProgramHeaderRegion> data_region;
    Optional<ProgramHeaderRegion> tls_region;
    VirtualAddress dynamic_region_desired_vaddr;

    elf_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        ProgramHeaderRegion new_region;
        new_region.set_program_header(program_header.raw_header());
        program_headers.append(move(new_region));
        auto region = program_headers.last();
        if (region.is_tls_template()) {
            tls_region = region;
        } else if (region.is_load()) {
            if (region.is_executable()) {
                text_region = region;
            } else
                data_region = region;
        } else if (region.is_dynamic()) {
            dynamic_region_desired_vaddr = region.desired_load_address();
        }
    });

    ASSERT(text_region.has_value() && data_region.has_value());

    // Process regions in order: .text, .data, .tls
    void* text_segment_begin = mmap_with_name(nullptr, text_region.value().required_load_size(),
        text_region.value().mmap_prot(),
        MAP_PRIVATE,
        m_image_fd,
        text_region.value().offset(),
        String::format(".text: %s", m_filename.characters()).characters());

    if (MAP_FAILED == text_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    m_text_segment_size = text_region.value().required_load_size();
    m_text_segment_load_address = VirtualAddress { (u32)text_segment_begin };

    m_dynamic_section_address = dynamic_region_desired_vaddr.offset(m_text_segment_load_address.get());

    void* data_segment_begin = mmap_with_name((u8*)text_segment_begin + m_text_segment_size, data_region.value().required_load_size(), data_region.value().mmap_prot(), MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, String::format(".data: %s", m_filename.characters()).characters());
    if (MAP_FAILED == data_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    VirtualAddress data_segment_actual_addr = data_region.value().desired_load_address().offset((u32)text_segment_begin);
    memcpy(data_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + data_region.value().offset(), data_region.value().size_in_image());

    // FIXME: Do some kind of 'allocate TLS section' or some such from a per-application pool
    if (tls_region.has_value()) {
        // FIXME: This can't be right either. TLS needs some real work i'd say :)
        m_tls_segment_address = tls_region.value().desired_load_address();
        VirtualAddress tls_segment_actual_addr = tls_region.value().desired_load_address().offset((u32)text_segment_begin);
        dbg() << "copying TLS into: " << (void*)tls_segment_actual_addr.as_ptr();
        memcpy(tls_segment_actual_addr.as_ptr(),
            (u8*)m_file_mapping + tls_region.value().offset(),
            tls_region.value().size_in_image());
        dbg() << "after copy";
    }
}

void DynamicLoader::do_relocations()
{
    u32 load_base_address = m_dynamic_object->base_address().get();

    // FIXME: We should really bail on undefined symbols here.

    auto main_relocation_section = m_dynamic_object->relocation_section();

    main_relocation_section.for_each_relocation([&](const DynamicObject::Relocation& relocation) {
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
            u32 symbol_address = lookup_symbol(symbol).value();
            *patch_ptr += symbol_address;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_PC32: {
            auto symbol = relocation.symbol();
            VERBOSE("PC-relative relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            u32 relative_offset = (lookup_symbol(symbol).value() - (relocation.offset() + load_base_address));
            *patch_ptr += relative_offset;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_GLOB_DAT: {
            auto symbol = relocation.symbol();
            VERBOSE("Global data relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            u32 symbol_location = lookup_symbol(symbol).value();
            ASSERT(symbol_location != load_base_address);
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
            dbg() << "R_386_TLS_TPOFF: *patch_ptr: " << (void*)(*patch_ptr);
            auto symbol = relocation.symbol();
            VERBOSE("TLS relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            u32 symbol_location = lookup_symbol(symbol).value();
            VERBOSE("symbol location: %d\n", symbol_location);
            *patch_ptr = relocation.offset() - (u32)m_tls_segment_address.as_ptr() - *patch_ptr;
            break;
        }
        case R_386_TLS_DTPMOD3: {
            // FIXME
            dbg() << "TODO: Offset in TLS block";
            ASSERT_NOT_REACHED();
            break;
        }
        case R_386_TLS_DTPOFF32: {
            // FIXME
            dbg() << "TOODO: R_386_TLS_DTPOFF32";
            ASSERT_NOT_REACHED();
            break;
        }
        default:
            // Raise the alarm! Someone needs to implement this relocation type
            dbgprintf("Found a new exciting relocation type %d\n", relocation.type());
            printf("DynamicLoader: Found unknown relocation type %d\n", relocation.type());
            ASSERT_NOT_REACHED();
            break;
        }
        return IterationDecision::Continue;
    });

    // Handle PLT Global offset table relocations.
    m_dynamic_object->plt_relocation_section().for_each_relocation([&](const DynamicObject::Relocation& relocation) {
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

void DynamicLoader::setup_plt_trampoline()
{
    auto got_address = m_dynamic_object->plt_got_base_address();
    if (!got_address.has_value()) {
        dbg() << "no plt, exiting setup_plt_trampoline";
        return;
    }

    u32* got_u32_ptr = (u32*)got_address.value().as_ptr();
    got_u32_ptr[1] = (u32)this;
    got_u32_ptr[2] = (u32)&_plt_trampoline;

#ifdef DYNAMIC_LOAD_DEBUG
    dbgprintf("Set GOT PLT entries at %p: [0] = %p [1] = %p, [2] = %p\n", got_u32_ptr, got_u32_ptr[0], got_u32_ptr[1], got_u32_ptr[2]);
#endif
}

// void DynamicLoader::relocate_got_plt()
// {
//
// At this stage, the .got.plt contains addresses that point back to
// the plt. This is a part of the lazy linking mechanism.
// The plt was probably loaded somewhere else than the compiler expected,
// so we need to relocate the addresses in .got.plt to point at the actual
// indtended parts in the plt.

// TODO: is this actually the way it's supposed to be done?
// why doesn't the compiler explicitly generate relocations for this?

// auto got_address = m_dynamic_object->plt_got_base_address();
// if (!got_address.has_value()) {
//     dbg() << "no plt, exiting relocate_got_plt";
//     return;
// }
// u32* got_u32_ptr = (u32*)got_address.value().as_ptr();
// u32 num_plt_entries = m_dynamic_object->number_of_plt_relocation_entries();
// const size_t OFFSET_IN_GOT_PLT = 3; // The first three entries in .got.plt are artificial
// for (size_t i = 0; i < num_plt_entries; ++i) {
//     dbg() << "relocate plt entry #" << i;
//     dbg() << (void*)got_u32_ptr[i + OFFSET_IN_GOT_PLT] << "->" << (void*)(got_u32_ptr[i + OFFSET_IN_GOT_PLT] + m_dynamic_object->base_address().get());
//     got_u32_ptr[i + OFFSET_IN_GOT_PLT] += m_dynamic_object->base_address().get();
// }
// }

Optional<u32> DynamicLoader::lookup_symbol(const ELF::DynamicObject::Symbol& symbol) const
{
    // if (!symbol.is_undefined())
    //     return symbol.address().get();
    // TODO: check if symbol exists in .dynsym
    // dbg() << "lookup_symbol: " << symbol.name();
    // auto sym = symbol_for_name(symbol.name());
    // if (sym)
    //     return symbol.address().get();
    if (!symbol.is_undefined())
        return symbol.address().get();

    // TODO: Does a symbol with a null value always mean it is defined externally?
    dbg() << "Looking up symbol: " << symbol.name();
    auto res = dlsym(nullptr, symbol.name());
    if (!res)
        return {};
    ASSERT(res);
    return (u32)res;
}

// Called from our ASM routine _plt_trampoline
extern "C" Elf32_Addr _fixup_plt_entry(DynamicLoader* object, u32 relocation_offset)
{
    return object->patch_plt_entry(relocation_offset);
}

// offset is in PLT relocation table
Elf32_Addr DynamicLoader::patch_plt_entry(u32 relocation_offset)
{
    dbg() << "patch_plt_entry called with: " << relocation_offset;
    auto relocation = m_dynamic_object->plt_relocation_section().relocation_at_offset(relocation_offset);

    ASSERT(relocation.type() == R_386_JMP_SLOT);

    auto sym = relocation.symbol();

    u8* relocation_address = relocation.address().as_ptr();
    auto res = lookup_symbol(sym);
    ASSERT(res.has_value());
    u32 symbol_location = res.value();

    VERBOSE("DynamicLoader: Jump slot relocation: putting %s (%p) into PLT at %p\n", sym.name(), symbol_location, relocation_address);

    *(u32*)relocation_address = symbol_location;

    return symbol_location;
}

void DynamicLoader::call_object_init_functions()
{
    typedef void (*InitFunc)();
    if (m_dynamic_object->init_section().has_value()) {
        auto init_function = (InitFunc)(m_dynamic_object->init_section().value().address().as_ptr());

#ifdef DYNAMIC_LOAD_DEBUG
        dbgprintf("Calling DT_INIT at %p\n", init_function);
#endif
        (init_function)();
    }

    if (m_dynamic_object->init_array_section().has_value()) {
        auto init_array_section = m_dynamic_object->init_array_section().value();

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
}

u32 DynamicLoader::ProgramHeaderRegion::mmap_prot() const
{
    int prot = 0;
    prot |= is_executable() ? PROT_EXEC : 0;
    prot |= is_readable() ? PROT_READ : 0;
    prot |= is_writable() ? PROT_WRITE : 0;
    return prot;
}
} // end namespace ELF
