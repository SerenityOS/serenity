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

#include <AK/Debug.h>
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/Validation.h>
#include <assert.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>

#ifndef __serenity__
static void* mmap_with_name(void* addr, size_t length, int prot, int flags, int fd, off_t offset, const char*)
{
    return mmap(addr, length, prot, flags, fd, offset);
}

#    define MAP_RANDOMIZED 0
#endif

namespace ELF {

static bool s_always_bind_now = false;

RefPtr<DynamicLoader> DynamicLoader::try_create(int fd, String filename)
{
    struct stat stat;
    if (fstat(fd, &stat) < 0) {
        perror("DynamicLoader::try_create fstat");
        return {};
    }

    ASSERT(stat.st_size >= 0);
    size_t size = static_cast<size_t>(stat.st_size);
    if (size < sizeof(Elf32_Ehdr))
        return {};

    String file_mmap_name = String::formatted("ELF_DYN: {}", filename);
    auto* data = mmap_with_name(nullptr, size, PROT_READ, MAP_PRIVATE, fd, 0, file_mmap_name.characters());
    if (data == MAP_FAILED) {
        perror("DynamicLoader::try_create mmap");
        return {};
    }

    return adopt(*new DynamicLoader(fd, move(filename), data, size));
}

DynamicLoader::DynamicLoader(int fd, String filename, void* data, size_t size)
    : m_filename(move(filename))
    , m_file_size(size)
    , m_image_fd(fd)
    , m_file_data(data)
    , m_elf_image((u8*)m_file_data, m_file_size)
{
    m_tls_size = calculate_tls_size();
    m_valid = validate();
}

DynamicLoader::~DynamicLoader()
{
    if (munmap(m_file_data, m_file_size) < 0) {
        perror("munmap");
        ASSERT_NOT_REACHED();
    }
    if (close(m_image_fd) < 0) {
        perror("close");
        ASSERT_NOT_REACHED();
    }
}

const DynamicObject& DynamicLoader::dynamic_object() const
{
    if (!m_cached_dynamic_object) {
        VirtualAddress dynamic_section_address;

        m_elf_image.for_each_program_header([&dynamic_section_address](auto program_header) {
            if (program_header.type() == PT_DYNAMIC) {
                dynamic_section_address = VirtualAddress(program_header.raw_data());
            }
            return IterationDecision::Continue;
        });
        ASSERT(!dynamic_section_address.is_null());

        m_cached_dynamic_object = ELF::DynamicObject::construct(VirtualAddress(m_elf_image.base_address()), dynamic_section_address);
    }
    return *m_cached_dynamic_object;
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
    if (!m_elf_image.is_valid())
        return false;

    auto* elf_header = (Elf32_Ehdr*)m_file_data;
    if (!validate_elf_header(*elf_header, m_file_size))
        return false;
    if (!validate_program_headers(*elf_header, m_file_size, (u8*)m_file_data, m_file_size, &m_program_interpreter))
        return false;
    return true;
}

void* DynamicLoader::symbol_for_name(const char* name)
{
    auto symbol = m_dynamic_object->hash_section().lookup_symbol(name);

    if (symbol.is_undefined())
        return nullptr;

    return m_dynamic_object->base_address().offset(symbol.value()).as_ptr();
}

RefPtr<DynamicObject> DynamicLoader::map()
{
    ASSERT(!m_dynamic_object);

    if (!m_valid) {
        dbgln("DynamicLoader::map failed: image is invalid");
        return nullptr;
    }

    load_program_headers();

    m_dynamic_object = DynamicObject::construct(m_text_segment_load_address, m_dynamic_section_address);
    m_dynamic_object->set_tls_offset(m_tls_offset);
    m_dynamic_object->set_tls_size(m_tls_size);

    return m_dynamic_object;
}

bool DynamicLoader::link(unsigned flags, size_t total_tls_size)
{
    return load_stage_2(flags, total_tls_size);
}

bool DynamicLoader::load_stage_2(unsigned flags, size_t total_tls_size)
{
    ASSERT(flags & RTLD_GLOBAL);

#if DYNAMIC_LOAD_DEBUG
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
        if (m_dynamic_object->has_plt())
            setup_plt_trampoline();
    }

    if (mprotect(m_text_segment_load_address.as_ptr(), m_text_segment_size, PROT_READ | PROT_EXEC) < 0) {
        perror("mprotect .text: PROT_READ | PROT_EXEC"); // FIXME: dlerror?
        return nullptr;
    }

    if (m_relro_segment_size) {
        if (mprotect(m_relro_segment_address.as_ptr(), m_relro_segment_size, PROT_READ) < 0) {
            perror("mprotect .relro: PROT_READ");
            return nullptr;
        }

#if __serenity__
        if (set_mmap_name(m_relro_segment_address.as_ptr(), m_relro_segment_size, String::formatted("{}: .relro", m_filename).characters()) < 0) {
            perror("set_mmap_name .relro");
            return nullptr;
        }
#endif
    }

    call_object_init_functions();

    dbgln_if(DYNAMIC_LOAD_DEBUG, "Loaded {}", m_filename);
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
    Optional<ProgramHeaderRegion> text_region;
    Optional<ProgramHeaderRegion> data_region;
    Optional<ProgramHeaderRegion> tls_region;
    Optional<ProgramHeaderRegion> relro_region;

    VirtualAddress dynamic_region_desired_vaddr;

    m_elf_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        ProgramHeaderRegion region {};
        region.set_program_header(program_header.raw_header());
        if (region.is_tls_template()) {
            ASSERT(!tls_region.has_value());
            tls_region = region;
        } else if (region.is_load()) {
            if (region.is_executable()) {
                ASSERT(!text_region.has_value());
                text_region = region;
            } else {
                ASSERT(!data_region.has_value());
                data_region = region;
            }
        } else if (region.is_dynamic()) {
            dynamic_region_desired_vaddr = region.desired_load_address();
        } else if (region.is_relro()) {
            ASSERT(!relro_region.has_value());
            relro_region = region;
        }
        return IterationDecision::Continue;
    });

    ASSERT(text_region.has_value());
    ASSERT(data_region.has_value());

    // Process regions in order: .text, .data, .tls
    void* requested_load_address = m_elf_image.is_dynamic() ? nullptr : text_region.value().desired_load_address().as_ptr();

    int reservation_mmap_flags = MAP_ANON | MAP_PRIVATE | MAP_NORESERVE;
    if (m_elf_image.is_dynamic())
        reservation_mmap_flags |= MAP_RANDOMIZED;
    else
        reservation_mmap_flags |= MAP_FIXED;

    ASSERT(!text_region.value().is_writable());

    // First, we make a dummy reservation mapping, in order to allocate enough VM
    // to hold both text+data contiguously in the address space.

    FlatPtr ph_text_base = text_region.value().desired_load_address().page_base().get();
    FlatPtr ph_text_end = round_up_to_power_of_two(text_region.value().desired_load_address().offset(text_region.value().size_in_memory()).get(), PAGE_SIZE);
    FlatPtr ph_data_base = data_region.value().desired_load_address().page_base().get();
    FlatPtr ph_data_end = round_up_to_power_of_two(data_region.value().desired_load_address().offset(data_region.value().size_in_memory()).get(), PAGE_SIZE);

    size_t total_mapping_size = ph_data_end - ph_text_base;

    size_t text_segment_size = ph_text_end - ph_text_base;
    size_t data_segment_size = ph_data_end - ph_data_base;

    auto* reservation = mmap(requested_load_address, total_mapping_size, PROT_NONE, reservation_mmap_flags, 0, 0);
    if (reservation == MAP_FAILED) {
        perror("mmap reservation");
        ASSERT_NOT_REACHED();
    }

    // Then we unmap the reservation.
    if (munmap(reservation, total_mapping_size) < 0) {
        perror("munmap reservation");
        ASSERT_NOT_REACHED();
    }

    // Now we can map the text segment at the reserved address.
    auto* text_segment_begin = (u8*)mmap_with_name(
        reservation,
        text_segment_size,
        PROT_READ,
        MAP_FILE | MAP_SHARED | MAP_FIXED,
        m_image_fd,
        text_region.value().offset(),
        String::formatted("{}: .text", m_filename).characters());

    if (text_segment_begin == MAP_FAILED) {
        perror("mmap text");
        ASSERT_NOT_REACHED();
    }

    ASSERT(requested_load_address == nullptr || requested_load_address == text_segment_begin);
    m_text_segment_size = text_segment_size;
    m_text_segment_load_address = VirtualAddress { (FlatPtr)text_segment_begin };

    if (relro_region.has_value()) {
        m_relro_segment_size = relro_region->size_in_memory();
        m_relro_segment_address = m_text_segment_load_address.offset(relro_region->desired_load_address().get());
    }

    if (m_elf_image.is_dynamic())
        m_dynamic_section_address = dynamic_region_desired_vaddr.offset(m_text_segment_load_address.get());
    else
        m_dynamic_section_address = dynamic_region_desired_vaddr;

    FlatPtr data_segment_offset_from_text = ph_data_base - ph_text_base;

    // Finally, we make an anonymous mapping for the data segment. Contents are then copied from the file.
    auto* data_segment_address = (u8*)text_segment_begin + data_segment_offset_from_text;

    auto* data_segment = (u8*)mmap_with_name(
        data_segment_address,
        data_segment_size,
        PROT_READ | PROT_WRITE,
        MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED,
        0,
        0,
        String::formatted("{}: .data", m_filename).characters());

    if (MAP_FAILED == data_segment) {
        perror("mmap data");
        ASSERT_NOT_REACHED();
    }

    VirtualAddress data_segment_start;
    if (m_elf_image.is_dynamic())
        data_segment_start = data_region.value().desired_load_address().offset((FlatPtr)text_segment_begin);
    else
        data_segment_start = data_region.value().desired_load_address();

    memcpy(data_segment_start.as_ptr(), (u8*)m_file_data + data_region.value().offset(), data_region.value().size_in_image());

    // FIXME: Initialize the values in the TLS section. Currently, it is zeroed.
}

DynamicLoader::RelocationResult DynamicLoader::do_relocation(size_t total_tls_size, ELF::DynamicObject::Relocation relocation)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "Relocation symbol: {}, type: {}", relocation.symbol().name(), relocation.type());
    FlatPtr* patch_ptr = nullptr;
    if (is_dynamic())
        patch_ptr = (FlatPtr*)(m_dynamic_object->base_address().as_ptr() + relocation.offset());
    else
        patch_ptr = (FlatPtr*)(FlatPtr)relocation.offset();

    dbgln_if(DYNAMIC_LOAD_DEBUG, "dynamic object base address: {:p}", m_dynamic_object->base_address().as_ptr());
    dbgln_if(DYNAMIC_LOAD_DEBUG, "relocation offset: {:#08x}", relocation.offset());
    dbgln_if(DYNAMIC_LOAD_DEBUG, "patch_ptr: {:p}", patch_ptr);
    switch (relocation.type()) {
    case R_386_NONE:
        // Apparently most loaders will just skip these?
        // Seems if the 'link editor' generates one something is funky with your code
        dbgln_if(DYNAMIC_LOAD_DEBUG, "None relocation. No symbol, no nothing.");
        break;
    case R_386_32: {
        auto symbol = relocation.symbol();
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Absolute relocation: name: '{}', value: {}", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.has_value()) {
            if (symbol.bind() == STB_WEAK)
                return RelocationResult::ResolveLater;
            dbgln("ERROR: symbol not found: {}.", symbol.name());
            ASSERT_NOT_REACHED();
        }
        u32 symbol_address = res.value().address;
        *patch_ptr += symbol_address;
        dbgln_if(DYNAMIC_LOAD_DEBUG, "   Symbol address: {:p}", *patch_ptr);
        break;
    }
    case R_386_PC32: {
        auto symbol = relocation.symbol();
        dbgln_if(DYNAMIC_LOAD_DEBUG, "PC-relative relocation: '{}', value: {:p}", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        ASSERT(res.has_value());
        u32 relative_offset = (res.value().address - (FlatPtr)(m_dynamic_object->base_address().as_ptr() + relocation.offset()));
        *patch_ptr += relative_offset;
        dbgln_if(DYNAMIC_LOAD_DEBUG, "   Symbol address: {:p}", *patch_ptr);
        break;
    }
    case R_386_GLOB_DAT: {
        auto symbol = relocation.symbol();
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Global data relocation: '{}', value: {:p}", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.has_value()) {
            // We do not support these
            // TODO: Can we tell gcc not to generate the piece of code that uses these?
            // (--disable-tm-clone-registry flag in gcc conifugraion?)
            if (!strcmp(symbol.name(), "__deregister_frame_info") || !strcmp(symbol.name(), "_ITM_registerTMCloneTable")
                || !strcmp(symbol.name(), "_ITM_deregisterTMCloneTable") || !strcmp(symbol.name(), "__register_frame_info")) {
                break;
            }

            if (symbol.bind() == STB_WEAK)
                return RelocationResult::ResolveLater;

            // Symbol not found
            return RelocationResult::Failed;
        }
        dbgln_if(DYNAMIC_LOAD_DEBUG, "symbol found, location: {:#08x}", res.value().address);
        dbgln_if(DYNAMIC_LOAD_DEBUG, "object: {}", m_filename);

        u32 symbol_location = res.value().address;
        ASSERT(symbol_location != (FlatPtr)m_dynamic_object->base_address().as_ptr());
        *patch_ptr = symbol_location;
        dbgln_if(DYNAMIC_LOAD_DEBUG, "   Symbol address: {:p}", *patch_ptr);
        break;
    }
    case R_386_RELATIVE: {
        // FIXME: According to the spec, R_386_relative ones must be done first.
        //     We could explicitly do them first using m_number_of_relocatoins from DT_RELCOUNT
        //     However, our compiler is nice enough to put them at the front of the relocations for us :)
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Load address relocation at offset {:#08x}", relocation.offset());
        dbgln_if(DYNAMIC_LOAD_DEBUG, "    patch ptr == {:p}, adding load base address ({:p}) to it and storing {:p}", *patch_ptr, m_dynamic_object->base_address().as_ptr(), *patch_ptr + m_dynamic_object->base_address().as_ptr());
        *patch_ptr += (FlatPtr)m_dynamic_object->base_address().as_ptr(); // + addend for RelA (addend for Rel is stored at addr)
        break;
    }
    case R_386_TLS_TPOFF32:
    case R_386_TLS_TPOFF: {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Relocation type: R_386_TLS_TPOFF at offset {:#08x}", relocation.offset());
        auto symbol = relocation.symbol();
        // For some reason, LibC has a R_386_TLS_TPOFF that refers to the undefined symbol.. huh
        if (relocation.symbol_index() == 0)
            break;
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Symbol index: {}", symbol.index());
        dbgln_if(DYNAMIC_LOAD_DEBUG, "Symbol is_undefined?: {}", symbol.is_undefined());
        dbgln_if(DYNAMIC_LOAD_DEBUG, "TLS relocation: '{}', value: {:p}", symbol.name(), symbol.value());
        auto res = lookup_symbol(symbol);
        if (!res.has_value())
            break;
        u32 symbol_value = res.value().value;
        dbgln_if(DYNAMIC_LOAD_DEBUG, "symbol value: {}", symbol_value);
        auto* dynamic_object_of_symbol = res.value().dynamic_object;
        ASSERT(dynamic_object_of_symbol);
        size_t offset_of_tls_end = dynamic_object_of_symbol->tls_offset().value() + dynamic_object_of_symbol->tls_size().value();
        dbgln_if(DYNAMIC_LOAD_DEBUG, "patch ptr: {:p}", patch_ptr);
        dbgln_if(DYNAMIC_LOAD_DEBUG, "tls end offset: {}, total tls size: {}", offset_of_tls_end, total_tls_size);
        *patch_ptr = (offset_of_tls_end - total_tls_size - symbol_value - sizeof(Elf32_Addr));
        dbgln_if(DYNAMIC_LOAD_DEBUG, "*patch ptr: {}", (i32)*patch_ptr);
        break;
    }
    case R_386_JMP_SLOT: {
        // FIXME: Or BIND_NOW flag passed in?
        if (m_dynamic_object->must_bind_now() || s_always_bind_now) {
            // Eagerly BIND_NOW the PLT entries, doing all the symbol looking goodness
            // The patch method returns the address for the LAZY fixup path, but we don't need it here
            dbgln_if(DYNAMIC_LOAD_DEBUG, "patching plt reloaction: {:p}", relocation.offset_in_section());
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
        dbgln("Found a new exciting relocation type {}", relocation.type());
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
    ASSERT(m_dynamic_object->has_plt());
    VirtualAddress got_address = m_dynamic_object->plt_got_base_address();

    FlatPtr* got_ptr = (FlatPtr*)got_address.as_ptr();
    got_ptr[1] = (FlatPtr)m_dynamic_object.ptr();
    got_ptr[2] = (FlatPtr)&_plt_trampoline;

    dbgln_if(DYNAMIC_LOAD_DEBUG, "Set GOT PLT entries at {:p}: [0] = {:p} [1] = {:p}, [2] = {:p}", got_ptr, (void*)got_ptr[0], (void*)got_ptr[1], (void*)got_ptr[2]);
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

        dbgln_if(DYNAMIC_LOAD_DEBUG, "Calling DT_INIT at {:p}", init_function);
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
            dbgln_if(DYNAMIC_LOAD_DEBUG, "Calling DT_INITARRAY entry at {:p}", *init_begin);
            (*init_begin)();
            ++init_begin;
        }
    }
}

Optional<DynamicObject::SymbolLookupResult> DynamicLoader::lookup_symbol(const ELF::DynamicObject::Symbol& symbol) const
{
    return m_dynamic_object->lookup_symbol(symbol);
}

} // end namespace ELF
