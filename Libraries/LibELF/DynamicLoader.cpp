/*
 * Copyright (c) 2019-2020, Andrew Kaster <andrewdkaster@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include <AK/StringBuilder.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/Validation.h>

#include <assert.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>

#ifndef DYNAMIC_LOAD_DEBUG
#    define DYNAMIC_LOAD_DEBUG
#endif

// #define DYNAMIC_LOAD_VERBOSE

#ifdef DYNAMIC_LOAD_VERBOSE
#    define VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)
#else
#    define VERBOSE(fmt, ...) \
        do {                  \
        } while (0)
#endif

#ifndef __serenity__
static void* mmap_with_name(void* addr, size_t length, int prot, int flags, int fd, off_t offset, const char*)
{
    return mmap(addr, length, prot, flags, fd, offset);
}
#endif

namespace ELF {

static bool s_always_bind_now = false;

NonnullRefPtr<DynamicLoader> DynamicLoader::construct(const char* filename, int fd, size_t size)
{
    return adopt(*new DynamicLoader(filename, fd, size));
}

void* DynamicLoader::do_mmap(int fd, size_t size, const String& name)
{
    if (size < sizeof(Elf32_Ehdr))
        return MAP_FAILED;

    String file_mmap_name = String::format("ELF_DYN: %s", name.characters());
    return mmap_with_name(nullptr, size, PROT_READ, MAP_PRIVATE, fd, 0, file_mmap_name.characters());
}

DynamicLoader::DynamicLoader(const char* filename, int fd, size_t size)
    : m_filename(filename)
    , m_file_size(size)
    , m_image_fd(fd)
    , m_file_mapping(do_mmap(m_image_fd, m_file_size, m_filename))
    , m_elf_image((u8*)m_file_mapping, m_file_size)
{

    if (m_file_mapping == MAP_FAILED) {
        m_valid = false;
        return;
    }

    m_tls_size = calculate_tls_size();

    m_valid = validate();
}

RefPtr<DynamicObject> DynamicLoader::dynamic_object_from_image() const
{
    VirtualAddress dynamic_section_address;

    m_elf_image.for_each_program_header([&dynamic_section_address](auto program_header) {
        if (program_header.type() == PT_DYNAMIC) {
            dynamic_section_address = VirtualAddress(program_header.raw_data());
        }
        return IterationDecision::Continue;
    });
    ASSERT(!dynamic_section_address.is_null());

    return ELF::DynamicObject::construct(VirtualAddress(m_elf_image.base_address()), dynamic_section_address);
}

size_t DynamicLoader::calculate_tls_size() const
{
    size_t tls_size = 0;
    m_elf_image.for_each_program_header([&tls_size](auto program_header) {
        if (program_header.type() == PT_TLS) {
            tls_size = program_header.size_in_memory();
        }
        return IterationDecision::Continue;
    });
    return tls_size;
}

bool DynamicLoader::validate()
{
    auto* elf_header = (Elf32_Ehdr*)m_file_mapping;
    return validate_elf_header(*elf_header, m_file_size) && validate_program_headers(*elf_header, m_file_size, (u8*)m_file_mapping, m_file_size, &m_program_interpreter);
}

DynamicLoader::~DynamicLoader()
{
    if (MAP_FAILED != m_file_mapping)
        munmap(m_file_mapping, m_file_size);
    close(m_image_fd);
}

void* DynamicLoader::symbol_for_name(const char* name)
{
    auto symbol = m_dynamic_object->hash_section().lookup_symbol(name);

    if (symbol.is_undefined())
        return nullptr;

    return m_dynamic_object->base_address().offset(symbol.value()).as_ptr();
}

RefPtr<DynamicObject> DynamicLoader::load_from_image(unsigned flags, size_t total_tls_size)
{

    m_valid = m_elf_image.is_valid();

    if (!m_valid) {
        dbgprintf("DynamicLoader::load_from_image failed: image is invalid\n");
        return nullptr;
    }

#ifdef DYNAMIC_LOAD_VERBOSE
    // m_image->dump();
#endif

    load_program_headers();

    m_dynamic_object = DynamicObject::construct(m_text_segment_load_address, m_dynamic_section_address);
    m_dynamic_object->set_tls_offset(m_tls_offset);
    m_dynamic_object->set_tls_size(m_tls_size);

    auto rc = load_stage_2(flags, total_tls_size);
    if (!rc) {
        dbgprintf("DynamicLoader::load_from_image failed at load_stage_2\n");
        return nullptr;
    }
    return m_dynamic_object;
}

bool DynamicLoader::load_stage_2(unsigned flags, size_t total_tls_size)
{
    ASSERT(flags & RTLD_GLOBAL);

#ifdef DYNAMIC_LOAD_DEBUG
    m_dynamic_object->dump();
#endif

    if (m_dynamic_object->has_text_relocations()) {
        ASSERT(m_text_segment_load_address.get() != 0);

#ifndef AK_OS_MACOS
        // Remap this text region as private.
        if (mremap(m_text_segment_load_address.as_ptr(), m_text_segment_size, m_text_segment_size, MAP_PRIVATE) == MAP_FAILED) {
            perror("mremap .text: MAP_PRIVATE");
            return false;
        }
#endif

        if (0 > mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_WRITE)) {
            perror("mprotect .text: PROT_READ | PROT_WRITE"); // FIXME: dlerror?
            return false;
        }
    }
    do_main_relocations(total_tls_size);
    return true;
}

void DynamicLoader::do_main_relocations(size_t total_tls_size)
{
    auto do_single_relocation = [&](ELF::DynamicObject::Relocation relocation) {
        switch (do_relocation(total_tls_size, relocation)) {
        case RelocationResult::Failed:
            dbgln("Loader.so: {} unresolved symbol '{}'", m_filename, relocation.symbol().name());
            ASSERT_NOT_REACHED();
            break;
        case RelocationResult::ResolveLater:
            m_unresolved_relocations.append(relocation);
            break;
        case RelocationResult::Success:
            break;
        }
        return IterationDecision::Continue;
    };
    m_dynamic_object->relocation_section().for_each_relocation(do_single_relocation);
    m_dynamic_object->plt_relocation_section().for_each_relocation(do_single_relocation);
}

RefPtr<DynamicObject> DynamicLoader::load_stage_3(unsigned flags, size_t total_tls_size)
{

    do_lazy_relocations(total_tls_size);
    if (flags & RTLD_LAZY) {
        setup_plt_trampoline();
    }

    // Clean up our setting of .text to PROT_READ | PROT_WRITE
    if (m_dynamic_object->has_text_relocations()) {
        if (0 > mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_EXEC)) {
            perror("mprotect .text: PROT_READ | PROT_EXEC"); // FIXME: dlerror?
            return nullptr;
        }
    }

    call_object_init_functions();

    VERBOSE("Loaded %s\n", m_filename.characters());
    return m_dynamic_object;
}

void DynamicLoader::do_lazy_relocations(size_t total_tls_size)
{
    for (const auto& relocation : m_unresolved_relocations) {
        if (auto res = do_relocation(total_tls_size, relocation); res != RelocationResult::Success) {
            dbgln("Loader.so: {} unresolved symbol '{}'", m_filename, relocation.symbol().name());
            ASSERT_NOT_REACHED();
        }
    }
}

void DynamicLoader::load_program_headers()
{
    Vector<ProgramHeaderRegion> program_headers;

    ProgramHeaderRegion* text_region_ptr = nullptr;
    ProgramHeaderRegion* data_region_ptr = nullptr;
    ProgramHeaderRegion* tls_region_ptr = nullptr;
    VirtualAddress dynamic_region_desired_vaddr;

    m_elf_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        ProgramHeaderRegion new_region;
        new_region.set_program_header(program_header.raw_header());
        program_headers.append(move(new_region));
        auto& region = program_headers.last();
        if (region.is_tls_template())
            tls_region_ptr = &region;
        else if (region.is_load()) {
            if (region.is_executable())
                text_region_ptr = &region;
            else
                data_region_ptr = &region;
        } else if (region.is_dynamic()) {
            dynamic_region_desired_vaddr = region.desired_load_address();
        }
        return IterationDecision::Continue;
    });

    ASSERT(text_region_ptr && data_region_ptr);

    // Process regions in order: .text, .data, .tls
    auto* region = text_region_ptr;
    void* requested_load_address = m_elf_image.is_dynamic() ? nullptr : region->desired_load_address().as_ptr();

    ASSERT(!region->is_writable());

    void* text_segment_begin = mmap_with_name(
        requested_load_address,
        region->required_load_size(),
        region->mmap_prot(),
        MAP_SHARED,
        m_image_fd,
        region->offset(),
        String::format("%s: .text", m_filename.characters()).characters());
    if (MAP_FAILED == text_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    ASSERT(requested_load_address == nullptr || requested_load_address == text_segment_begin);
    m_text_segment_size = region->required_load_size();
    m_text_segment_load_address = VirtualAddress { (FlatPtr)text_segment_begin };

    if (m_elf_image.is_dynamic())
        m_dynamic_section_address = dynamic_region_desired_vaddr.offset(m_text_segment_load_address.get());
    else
        m_dynamic_section_address = dynamic_region_desired_vaddr;

    region = data_region_ptr;
    void* data_segment_begin = mmap_with_name(
        (u8*)text_segment_begin + m_text_segment_size,
        region->required_load_size(),
        region->mmap_prot(),
        MAP_ANONYMOUS | MAP_PRIVATE,
        0,
        0,
        String::format("%s: .data", m_filename.characters()).characters());
    if (MAP_FAILED == data_segment_begin) {
        ASSERT_NOT_REACHED();
    }
    VirtualAddress data_segment_actual_addr;
    if (m_elf_image.is_dynamic()) {
        data_segment_actual_addr = region->desired_load_address().offset((FlatPtr)text_segment_begin);
    } else {
        data_segment_actual_addr = region->desired_load_address();
    }
    memcpy(data_segment_actual_addr.as_ptr(), (u8*)m_file_mapping + region->offset(), region->size_in_image());
    // FIXME: Initialize the values in the TLS section. Currently, it is zeroed.
}

DynamicLoader::RelocationResult DynamicLoader::do_relocation(size_t total_tls_size, ELF::DynamicObject::Relocation relocation)
{
    VERBOSE("Relocation symbol: %s, type: %d\n", relocation.symbol().name(), relocation.type());
    FlatPtr* patch_ptr = nullptr;
    if (is_dynamic())
        patch_ptr = (FlatPtr*)(m_dynamic_object->base_address().as_ptr() + relocation.offset());
    else
        patch_ptr = (FlatPtr*)(FlatPtr)relocation.offset();

    // VERBOSE("dynamic object name: %s\n", dynamic_object.object_name());
    VERBOSE("dynamic object base address: %p\n", m_dynamic_object->base_address());
    VERBOSE("relocation offset: 0x%x\n", relocation.offset());
    VERBOSE("patch_ptr: %p\n", patch_ptr);
    switch (relocation.type()) {
    case R_386_NONE:
        // Apparently most loaders will just skip these?
        // Seems if the 'link editor' generates one something is funky with your code
        VERBOSE("None relocation. No symbol, no nothing.\n");
        break;
    case R_386_32: {
        auto symbol = relocation.symbol();
        VERBOSE("Absolute relocation: name: '%s', value: %p\n", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.found) {
            if (symbol.bind() == STB_WEAK) {
                return RelocationResult::ResolveLater;
            }
            dbgln("ERROR: symbol not found: {}.", symbol.name());
            ASSERT_NOT_REACHED();
        }
        u32 symbol_address = res.address;
        *patch_ptr += symbol_address;
        VERBOSE("   Symbol address: %p\n", *patch_ptr);
        break;
    }
    case R_386_PC32: {
        auto symbol = relocation.symbol();
        VERBOSE("PC-relative relocation: '%s', value: %p\n", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        ASSERT(res.found);
        u32 relative_offset = (res.address - (FlatPtr)(m_dynamic_object->base_address().as_ptr() + relocation.offset()));
        *patch_ptr += relative_offset;
        VERBOSE("   Symbol address: %p\n", *patch_ptr);
        break;
    }
    case R_386_GLOB_DAT: {
        auto symbol = relocation.symbol();
        VERBOSE("Global data relocation: '%s', value: %p\n", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.found) {
            // We do not support these
            // TODO: Can we tell gcc not to generate the piece of code that uses these?
            // (--disable-tm-clone-registry flag in gcc conifugraion?)
            if (!strcmp(symbol.name(), "__deregister_frame_info") || !strcmp(symbol.name(), "_ITM_registerTMCloneTable")
                || !strcmp(symbol.name(), "_ITM_deregisterTMCloneTable") || !strcmp(symbol.name(), "__register_frame_info")) {
                break;
            }

            if (symbol.bind() == STB_WEAK) {
                return RelocationResult::ResolveLater;
            }

            // Symbol not found
            return RelocationResult::Failed;
        }
        VERBOSE("was symbol found? %d, address: 0x%x\n", res.found, res.address);
        VERBOSE("object: %s\n", m_filename.characters());

        u32 symbol_location = res.address;
        ASSERT(symbol_location != (FlatPtr)m_dynamic_object->base_address().as_ptr());
        *patch_ptr = symbol_location;
        VERBOSE("   Symbol address: %p\n", *patch_ptr);
        break;
    }
    case R_386_RELATIVE: {
        // FIXME: According to the spec, R_386_relative ones must be done first.
        //     We could explicitly do them first using m_number_of_relocatoins from DT_RELCOUNT
        //     However, our compiler is nice enough to put them at the front of the relocations for us :)
        VERBOSE("Load address relocation at offset %X\n", relocation.offset());
        VERBOSE("    patch ptr == %p, adding load base address (%p) to it and storing %p\n", *patch_ptr, m_dynamic_object->base_address().as_ptr(), *patch_ptr + m_dynamic_object->base_address().as_ptr());
        *patch_ptr += (FlatPtr)m_dynamic_object->base_address().as_ptr(); // + addend for RelA (addend for Rel is stored at addr)
        break;
    }
    case R_386_TLS_TPOFF32:
    case R_386_TLS_TPOFF: {
        VERBOSE("Relocation type: R_386_TLS_TPOFF at offset %X\n", relocation.offset());
        auto symbol = relocation.symbol();
        // For some reason, LibC has a R_386_TLS_TPOFF that refers to the undefined symbol.. huh
        if (relocation.symbol_index() == 0)
            break;
        VERBOSE("Symbol index: %d\n", symbol.index());
        VERBOSE("Symbol is_undefined?: %d\n", symbol.is_undefined());
        VERBOSE("TLS relocation: '%s', value: %p\n", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.found)
            break;
        ASSERT(res.found);
        u32 symbol_value = res.value;
        VERBOSE("symbol value: %d\n", symbol_value);
        const auto dynamic_object_of_symbol = res.dynamic_object;
        ASSERT(dynamic_object_of_symbol);
        size_t offset_of_tls_end = dynamic_object_of_symbol->tls_offset().value() + dynamic_object_of_symbol->tls_size().value();
        // size_t offset_of_tls_end = tls_offset() + tls_size();
        VERBOSE("patch ptr: 0x%x\n", patch_ptr);
        VERBOSE("tls end offset: %d, total tls size: %d\n", offset_of_tls_end, total_tls_size);
        *patch_ptr = (offset_of_tls_end - total_tls_size - symbol_value - sizeof(Elf32_Addr));
        VERBOSE("*patch ptr: %d\n", (i32)*patch_ptr);
        break;
    }
    case R_386_JMP_SLOT: {
        // FIXME: Or BIND_NOW flag passed in?
        if (m_dynamic_object->must_bind_now() || s_always_bind_now) {
            // Eagerly BIND_NOW the PLT entries, doing all the symbol looking goodness
            // The patch method returns the address for the LAZY fixup path, but we don't need it here
            VERBOSE("patching plt reloaction: 0x%x\n", relocation.offset_in_section());
            [[maybe_unused]] auto rc = m_dynamic_object->patch_plt_entry(relocation.offset_in_section());
        } else {
            u8* relocation_address = relocation.address().as_ptr();

            if (m_elf_image.is_dynamic())
                *(u32*)relocation_address += (FlatPtr)m_dynamic_object->base_address().as_ptr();
        }
        break;
    }
    default:
        // Raise the alarm! Someone needs to implement this relocation type
        VERBOSE("Found a new exciting relocation type %d\n", relocation.type());
        // printf("DynamicLoader: Found unknown relocation type %d\n", relocation.type());
        ASSERT_NOT_REACHED();
        break;
    }
    return RelocationResult::Success;
}

// Defined in <arch>/plt_trampoline.S
extern "C" void _plt_trampoline(void) __attribute__((visibility("hidden")));

void DynamicLoader::setup_plt_trampoline()
{
    ASSERT(m_dynamic_object);
    VirtualAddress got_address = m_dynamic_object->plt_got_base_address();

    FlatPtr* got_ptr = (FlatPtr*)got_address.as_ptr();
    got_ptr[1] = (FlatPtr)m_dynamic_object.ptr();
    got_ptr[2] = (FlatPtr)&_plt_trampoline;

    VERBOSE("Set GOT PLT entries at %p: [0] = %p [1] = %p, [2] = %p\n", got_ptr, (void*)got_ptr[0], (void*)got_ptr[1], (void*)got_ptr[2]);
}

// Called from our ASM routine _plt_trampoline.
// Tell the compiler that it might be called from other places:
extern "C" Elf32_Addr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset);
extern "C" Elf32_Addr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset)
{
    return object->patch_plt_entry(relocation_offset);
}

void DynamicLoader::call_object_init_functions()
{
    typedef void (*InitFunc)();

    if (m_dynamic_object->has_init_section()) {
        auto init_function = (InitFunc)(m_dynamic_object->init_section().address().as_ptr());

        VERBOSE("Calling DT_INIT at %p\n", init_function);
        (init_function)();
    }

    if (m_dynamic_object->has_init_array_section()) {
        auto init_array_section = m_dynamic_object->init_array_section();

        InitFunc* init_begin = (InitFunc*)(init_array_section.address().as_ptr());
        InitFunc* init_end = init_begin + init_array_section.entry_count();
        while (init_begin != init_end) {
            // Android sources claim that these can be -1, to be ignored.
            // 0 definitely shows up. Apparently 0/-1 are valid? Confusing.
            if (!*init_begin || ((FlatPtr)*init_begin == (FlatPtr)-1))
                continue;
            VERBOSE("Calling DT_INITARRAY entry at %p\n", *init_begin);
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

DynamicObject::SymbolLookupResult DynamicLoader::lookup_symbol(const ELF::DynamicObject::Symbol& symbol) const
{
    return m_dynamic_object->lookup_symbol(symbol);
}

} // end namespace ELF
