/*
 * Copyright (c) 2019-2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibDl/dlfcn.h>
#include <LibDl/dlfcn_integration.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/Hashes.h>
#include <LibELF/Validation.h>
#include <assert.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifndef __serenity__
static void* mmap_with_name(void* addr, size_t length, int prot, int flags, int fd, off_t offset, const char*)
{
    return mmap(addr, length, prot, flags, fd, offset);
}

#    define MAP_RANDOMIZED 0
#endif

namespace ELF {

Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> DynamicLoader::try_create(int fd, String filename)
{
    struct stat stat;
    if (fstat(fd, &stat) < 0) {
        return DlErrorMessage { "DynamicLoader::try_create fstat" };
    }

    VERIFY(stat.st_size >= 0);
    auto size = static_cast<size_t>(stat.st_size);
    if (size < sizeof(ElfW(Ehdr)))
        return DlErrorMessage { String::formatted("File {} has invalid ELF header", filename) };

    String file_mmap_name = String::formatted("ELF_DYN: {}", filename);
    auto* data = mmap_with_name(nullptr, size, PROT_READ, MAP_SHARED, fd, 0, file_mmap_name.characters());
    if (data == MAP_FAILED) {
        return DlErrorMessage { "DynamicLoader::try_create mmap" };
    }

    auto loader = adopt_ref(*new DynamicLoader(fd, move(filename), data, size));
    if (!loader->is_valid())
        return DlErrorMessage { "ELF image validation failed" };
    return loader;
}

DynamicLoader::DynamicLoader(int fd, String filename, void* data, size_t size)
    : m_filename(move(filename))
    , m_file_size(size)
    , m_image_fd(fd)
    , m_file_data(data)
    , m_elf_image((u8*)m_file_data, m_file_size)
{
    m_valid = validate();
    if (m_valid)
        m_tls_size_of_current_object = calculate_tls_size();
    else
        dbgln("Image validation failed for file {}", m_filename);
}

DynamicLoader::~DynamicLoader()
{
    if (munmap(m_file_data, m_file_size) < 0) {
        perror("munmap");
        VERIFY_NOT_REACHED();
    }
    if (close(m_image_fd) < 0) {
        perror("close");
        VERIFY_NOT_REACHED();
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
        });
        VERIFY(!dynamic_section_address.is_null());

        m_cached_dynamic_object = ELF::DynamicObject::create(m_filename, VirtualAddress(m_elf_image.base_address()), dynamic_section_address);
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
    });
    return tls_size;
}

bool DynamicLoader::validate()
{
    if (!m_elf_image.is_valid())
        return false;

    auto* elf_header = (ElfW(Ehdr)*)m_file_data;
    if (!validate_elf_header(*elf_header, m_file_size))
        return false;
    StringBuilder interpreter_path_builder;
    auto result_or_error = validate_program_headers(*elf_header, m_file_size, { m_file_data, m_file_size }, &interpreter_path_builder);
    if (result_or_error.is_error() || !result_or_error.value())
        return false;
    m_program_interpreter = interpreter_path_builder.string_view();
    return true;
}

RefPtr<DynamicObject> DynamicLoader::map()
{
    if (m_dynamic_object) {
        // Already mapped.
        return nullptr;
    }

    if (!m_valid) {
        dbgln("DynamicLoader::map failed: image is invalid");
        return nullptr;
    }

    load_program_headers();

    VERIFY(!m_base_address.is_null());

    m_dynamic_object = DynamicObject::create(m_filename, m_base_address, m_dynamic_section_address);
    m_dynamic_object->set_tls_offset(m_tls_offset);
    m_dynamic_object->set_tls_size(m_tls_size_of_current_object);

    return m_dynamic_object;
}

bool DynamicLoader::link(unsigned flags)
{
    return load_stage_2(flags);
}

bool DynamicLoader::load_stage_2(unsigned flags)
{
    VERIFY(flags & RTLD_GLOBAL);

    if (m_dynamic_object->has_text_relocations()) {
        for (auto& text_segment : m_text_segments) {
            VERIFY(text_segment.address().get() != 0);

#ifndef AK_OS_MACOS
            // Remap this text region as private.
            if (mremap(text_segment.address().as_ptr(), text_segment.size(), text_segment.size(), MAP_PRIVATE) == MAP_FAILED) {
                perror("mremap .text: MAP_PRIVATE");
                return false;
            }
#endif

            if (0 > mprotect(text_segment.address().as_ptr(), text_segment.size(), PROT_READ | PROT_WRITE)) {
                perror("mprotect .text: PROT_READ | PROT_WRITE"); // FIXME: dlerror?
                return false;
            }
        }
    }
    do_main_relocations();
    return true;
}

void DynamicLoader::do_main_relocations()
{
    auto do_single_relocation = [&](const ELF::DynamicObject::Relocation& relocation) {
        switch (do_relocation(relocation, ShouldInitializeWeak::No)) {
        case RelocationResult::Failed:
            dbgln("Loader.so: {} unresolved symbol '{}'", m_filename, relocation.symbol().name());
            VERIFY_NOT_REACHED();
        case RelocationResult::ResolveLater:
            m_unresolved_relocations.append(relocation);
            break;
        case RelocationResult::Success:
            break;
        }
    };

    m_dynamic_object->relocation_section().for_each_relocation(do_single_relocation);
    m_dynamic_object->plt_relocation_section().for_each_relocation(do_single_relocation);
    do_relr_relocations();
}

Result<NonnullRefPtr<DynamicObject>, DlErrorMessage> DynamicLoader::load_stage_3(unsigned flags)
{
    do_lazy_relocations();
    if (flags & RTLD_LAZY) {
        if (m_dynamic_object->has_plt())
            setup_plt_trampoline();
    }

    for (auto& text_segment : m_text_segments) {
        if (mprotect(text_segment.address().as_ptr(), text_segment.size(), PROT_READ | PROT_EXEC) < 0) {
            return DlErrorMessage { String::formatted("mprotect .text: PROT_READ | PROT_EXEC: {}", strerror(errno)) };
        }
    }

    if (m_relro_segment_size) {
        if (mprotect(m_relro_segment_address.as_ptr(), m_relro_segment_size, PROT_READ) < 0) {
            return DlErrorMessage { String::formatted("mprotect .relro: PROT_READ: {}", strerror(errno)) };
        }

#ifdef __serenity__
        if (set_mmap_name(m_relro_segment_address.as_ptr(), m_relro_segment_size, String::formatted("{}: .relro", m_filename).characters()) < 0) {
            return DlErrorMessage { String::formatted("set_mmap_name .relro: {}", strerror(errno)) };
        }
#endif
    }

    return NonnullRefPtr<DynamicObject> { *m_dynamic_object };
}

void DynamicLoader::load_stage_4()
{
    call_object_init_functions();
}

void DynamicLoader::do_lazy_relocations()
{
    for (const auto& relocation : m_unresolved_relocations) {
        if (auto res = do_relocation(relocation, ShouldInitializeWeak::Yes); res != RelocationResult::Success) {
            dbgln("Loader.so: {} unresolved symbol '{}'", m_filename, relocation.symbol().name());
            VERIFY_NOT_REACHED();
        }
    }
}

void DynamicLoader::load_program_headers()
{
    Vector<ProgramHeaderRegion> load_regions;
    Vector<ProgramHeaderRegion> map_regions;
    Vector<ProgramHeaderRegion> copy_regions;
    Optional<ProgramHeaderRegion> tls_region;
    Optional<ProgramHeaderRegion> relro_region;

    VirtualAddress dynamic_region_desired_vaddr;

    m_elf_image.for_each_program_header([&](const Image::ProgramHeader& program_header) {
        ProgramHeaderRegion region {};
        region.set_program_header(program_header.raw_header());
        if (region.is_tls_template()) {
            VERIFY(!tls_region.has_value());
            tls_region = region;
        } else if (region.is_load()) {
            if (region.size_in_memory() == 0)
                return;
            load_regions.append(region);
            if (region.is_writable()) {
                copy_regions.append(region);
            } else {
                map_regions.append(region);
            }
        } else if (region.is_dynamic()) {
            dynamic_region_desired_vaddr = region.desired_load_address();
        } else if (region.is_relro()) {
            VERIFY(!relro_region.has_value());
            relro_region = region;
        }
    });

    VERIFY(!map_regions.is_empty() || !copy_regions.is_empty());

    auto compare_load_address = [](ProgramHeaderRegion& a, ProgramHeaderRegion& b) {
        return a.desired_load_address().as_ptr() < b.desired_load_address().as_ptr();
    };

    quick_sort(load_regions, compare_load_address);
    quick_sort(map_regions, compare_load_address);
    quick_sort(copy_regions, compare_load_address);

    // Process regions in order: .text, .data, .tls
    void* requested_load_address = m_elf_image.is_dynamic() ? nullptr : load_regions.first().desired_load_address().as_ptr();

    int reservation_mmap_flags = MAP_ANON | MAP_PRIVATE | MAP_NORESERVE;
    if (m_elf_image.is_dynamic())
        reservation_mmap_flags |= MAP_RANDOMIZED;
#ifdef MAP_FIXED_NOREPLACE
    else
        reservation_mmap_flags |= MAP_FIXED_NOREPLACE;
#endif

    // First, we make a dummy reservation mapping, in order to allocate enough VM
    // to hold all regions contiguously in the address space.

    FlatPtr ph_load_base = load_regions.first().desired_load_address().page_base().get();
    FlatPtr ph_load_end = round_up_to_power_of_two(load_regions.last().desired_load_address().offset(load_regions.last().size_in_memory()).get(), PAGE_SIZE);

    size_t total_mapping_size = ph_load_end - ph_load_base;

    auto* reservation = mmap(requested_load_address, total_mapping_size, PROT_NONE, reservation_mmap_flags, 0, 0);
    if (reservation == MAP_FAILED) {
        perror("mmap reservation");
        VERIFY_NOT_REACHED();
    }

    VERIFY(requested_load_address == nullptr || reservation == requested_load_address);

    m_base_address = VirtualAddress { reservation };

    // Then we unmap the reservation.
    if (munmap(reservation, total_mapping_size) < 0) {
        perror("munmap reservation");
        VERIFY_NOT_REACHED();
    }

    for (auto& region : map_regions) {
        FlatPtr ph_desired_base = region.desired_load_address().get();
        FlatPtr ph_base = region.desired_load_address().page_base().get();
        FlatPtr ph_end = ph_base + round_up_to_power_of_two(region.size_in_memory() + region.desired_load_address().get() - ph_base, PAGE_SIZE);

        StringBuilder builder;
        builder.append(m_filename);
        if (region.is_executable())
            builder.append(": .text");
        else
            builder.append(": .rodata");

        // Now we can map the text segment at the reserved address.
        auto* segment_base = (u8*)mmap_with_name(
            (u8*)reservation + ph_base - ph_load_base,
            ph_desired_base - ph_base + region.size_in_image(),
            PROT_READ,
            MAP_FILE | MAP_SHARED | MAP_FIXED,
            m_image_fd,
            VirtualAddress { region.offset() }.page_base().get(),
            builder.to_string().characters());

        if (segment_base == MAP_FAILED) {
            perror("mmap non-writable");
            VERIFY_NOT_REACHED();
        }

        if (region.is_executable())
            m_text_segments.append({ VirtualAddress { segment_base }, ph_end - ph_base });
    }

    VERIFY(requested_load_address == nullptr || requested_load_address == reservation);

    if (relro_region.has_value()) {
        m_relro_segment_size = relro_region->size_in_memory();
        m_relro_segment_address = VirtualAddress { (u8*)reservation + relro_region->desired_load_address().get() - ph_load_base };
    }

    if (m_elf_image.is_dynamic())
        m_dynamic_section_address = VirtualAddress { (u8*)reservation + dynamic_region_desired_vaddr.get() - ph_load_base };
    else
        m_dynamic_section_address = dynamic_region_desired_vaddr;

    for (auto& region : copy_regions) {
        FlatPtr ph_data_base = region.desired_load_address().page_base().get();
        FlatPtr ph_data_end = ph_data_base + round_up_to_power_of_two(region.size_in_memory() + region.desired_load_address().get() - ph_data_base, PAGE_SIZE);

        auto* data_segment_address = (u8*)reservation + ph_data_base - ph_load_base;
        size_t data_segment_size = ph_data_end - ph_data_base;

        // Finally, we make an anonymous mapping for the data segment. Contents are then copied from the file.
        auto* data_segment = (u8*)mmap_with_name(
            data_segment_address,
            data_segment_size,
            PROT_READ | PROT_WRITE,
            MAP_ANONYMOUS | MAP_PRIVATE | MAP_FIXED,
            0,
            0,
            String::formatted("{}: .data", m_filename).characters());

        if (MAP_FAILED == data_segment) {
            perror("mmap writable");
            VERIFY_NOT_REACHED();
        }

        VirtualAddress data_segment_start;
        if (m_elf_image.is_dynamic())
            data_segment_start = VirtualAddress { (u8*)reservation + region.desired_load_address().get() };
        else
            data_segment_start = region.desired_load_address();

        VERIFY(data_segment_start.as_ptr() + region.size_in_memory() <= data_segment + data_segment_size);

        memcpy(data_segment_start.as_ptr(), (u8*)m_file_data + region.offset(), region.size_in_image());
    }

    // FIXME: Initialize the values in the TLS section. Currently, it is zeroed.
}

DynamicLoader::RelocationResult DynamicLoader::do_relocation(const ELF::DynamicObject::Relocation& relocation, ShouldInitializeWeak should_initialize_weak)
{
    FlatPtr* patch_ptr = nullptr;
    if (is_dynamic())
        patch_ptr = (FlatPtr*)(m_dynamic_object->base_address().as_ptr() + relocation.offset());
    else
        patch_ptr = (FlatPtr*)(FlatPtr)relocation.offset();

    switch (relocation.type()) {
#if ARCH(I386)
    case R_386_NONE:
#else
    case R_X86_64_NONE:
#endif
        // Apparently most loaders will just skip these?
        // Seems if the 'link editor' generates one something is funky with your code
        break;
#if ARCH(I386)
    case R_386_32: {
#else
    case R_X86_64_64: {
#endif
        auto symbol = relocation.symbol();
        auto res = lookup_symbol(symbol);
        if (!res.has_value()) {
            if (symbol.bind() == STB_WEAK)
                return RelocationResult::ResolveLater;
            dbgln("ERROR: symbol not found: {}.", symbol.name());
            return RelocationResult::Failed;
        }
        auto symbol_address = res.value().address;
        if (relocation.addend_used())
            *patch_ptr = symbol_address.get() + relocation.addend();
        else
            *patch_ptr += symbol_address.get();
        break;
    }
#if ARCH(I386)
    case R_386_PC32: {
        auto symbol = relocation.symbol();
        auto result = lookup_symbol(symbol);
        if (!result.has_value())
            return RelocationResult::Failed;
        auto relative_offset = result.value().address - m_dynamic_object->base_address().offset(relocation.offset());
        *patch_ptr += relative_offset.get();
        break;
    }
    case R_386_GLOB_DAT: {
#else
    case R_X86_64_GLOB_DAT: {
#endif
        auto symbol = relocation.symbol();
        auto res = lookup_symbol(symbol);
        VirtualAddress symbol_location;
        if (!res.has_value()) {
            if (symbol.bind() == STB_WEAK) {
                if (should_initialize_weak == ShouldInitializeWeak::No)
                    return RelocationResult::ResolveLater;
            } else {
                // Symbol not found
                return RelocationResult::Failed;
            }

            symbol_location = VirtualAddress { (FlatPtr)0 };
        } else
            symbol_location = res.value().address;
        VERIFY(symbol_location != m_dynamic_object->base_address());
        *patch_ptr = symbol_location.get();
        break;
    }
#if ARCH(I386)
    case R_386_RELATIVE: {
#else
    case R_X86_64_RELATIVE: {
#endif
        // FIXME: According to the spec, R_386_relative ones must be done first.
        //     We could explicitly do them first using m_number_of_relocations from DT_RELCOUNT
        //     However, our compiler is nice enough to put them at the front of the relocations for us :)
        if (relocation.addend_used())
            *patch_ptr = m_dynamic_object->base_address().offset(relocation.addend()).get();
        else
            *patch_ptr += m_dynamic_object->base_address().get();
        break;
    }
#if ARCH(I386)
    case R_386_TLS_TPOFF32:
    case R_386_TLS_TPOFF: {
#else
    case R_X86_64_TPOFF64: {
#endif
        auto symbol = relocation.symbol();
        FlatPtr symbol_value;
        DynamicObject const* dynamic_object_of_symbol;
        if (relocation.symbol_index() != 0) {
            auto res = lookup_symbol(symbol);
            if (!res.has_value())
                break;
            symbol_value = res.value().value;
            dynamic_object_of_symbol = res.value().dynamic_object;
        } else {
            symbol_value = 0;
            dynamic_object_of_symbol = &relocation.dynamic_object();
        }
        VERIFY(dynamic_object_of_symbol);
        size_t addend = relocation.addend_used() ? relocation.addend() : *patch_ptr;
        *patch_ptr = negative_offset_from_tls_block_end(dynamic_object_of_symbol->tls_offset().value(), symbol_value + addend);
        break;
    }
#if ARCH(I386)
    case R_386_JMP_SLOT: {
#else
    case R_X86_64_JUMP_SLOT: {
#endif
        // FIXME: Or BIND_NOW flag passed in?
        if (m_dynamic_object->must_bind_now()) {
            // Eagerly BIND_NOW the PLT entries, doing all the symbol looking goodness
            // The patch method returns the address for the LAZY fixup path, but we don't need it here
            m_dynamic_object->patch_plt_entry(relocation.offset_in_section());
        } else {
            auto relocation_address = (FlatPtr*)relocation.address().as_ptr();

            if (m_elf_image.is_dynamic())
                *relocation_address += m_dynamic_object->base_address().get();
        }
        break;
    }
    default:
        // Raise the alarm! Someone needs to implement this relocation type
        dbgln("Found a new exciting relocation type {}", relocation.type());
        VERIFY_NOT_REACHED();
    }
    return RelocationResult::Success;
}

void DynamicLoader::do_relr_relocations()
{
    auto base_address = m_dynamic_object->base_address().get();
    m_dynamic_object->for_each_relr_relocation([base_address](FlatPtr address) {
        *(FlatPtr*)address += base_address;
    });
}

ssize_t DynamicLoader::negative_offset_from_tls_block_end(ssize_t tls_offset, size_t value_of_symbol) const
{
    ssize_t offset = static_cast<ssize_t>(tls_offset + value_of_symbol);
    // At offset 0 there's the thread's ThreadSpecificData structure, we don't want to collide with it.
    VERIFY(offset < 0);
    return offset;
}

void DynamicLoader::copy_initial_tls_data_into(ByteBuffer& buffer) const
{
    const u8* tls_data = nullptr;
    size_t tls_size_in_image = 0;

    m_elf_image.for_each_program_header([this, &tls_data, &tls_size_in_image](ELF::Image::ProgramHeader program_header) {
        if (program_header.type() != PT_TLS)
            return IterationDecision::Continue;

        tls_data = (const u8*)m_file_data + program_header.offset();
        tls_size_in_image = program_header.size_in_image();
        return IterationDecision::Break;
    });

    if (!tls_data || !tls_size_in_image)
        return;

    m_elf_image.for_each_symbol([this, &buffer, tls_data](ELF::Image::Symbol symbol) {
        if (symbol.type() != STT_TLS)
            return IterationDecision::Continue;

        ssize_t negative_offset = negative_offset_from_tls_block_end(m_tls_offset, symbol.value());
        VERIFY(symbol.size() != 0);
        VERIFY(buffer.size() + negative_offset + symbol.size() <= buffer.size());
        memcpy(buffer.data() + buffer.size() + negative_offset, tls_data + symbol.value(), symbol.size());

        return IterationDecision::Continue;
    });
}

// Defined in <arch>/plt_trampoline.S
extern "C" void _plt_trampoline(void) __attribute__((visibility("hidden")));

void DynamicLoader::setup_plt_trampoline()
{
    VERIFY(m_dynamic_object);
    VERIFY(m_dynamic_object->has_plt());
    VirtualAddress got_address = m_dynamic_object->plt_got_base_address();

    auto* got_ptr = (FlatPtr*)got_address.as_ptr();
    got_ptr[1] = (FlatPtr)m_dynamic_object.ptr();
    got_ptr[2] = (FlatPtr)&_plt_trampoline;
}

// Called from our ASM routine _plt_trampoline.
// Tell the compiler that it might be called from other places:
extern "C" FlatPtr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset);
extern "C" FlatPtr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset)
{
    return object->patch_plt_entry(relocation_offset).get();
}

void DynamicLoader::call_object_init_functions()
{
    typedef void (*InitFunc)();

    if (m_dynamic_object->has_init_section()) {
        auto init_function = (InitFunc)(m_dynamic_object->init_section().address().as_ptr());
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
            (*init_begin)();
            ++init_begin;
        }
    }
}

Optional<DynamicObject::SymbolLookupResult> DynamicLoader::lookup_symbol(const ELF::DynamicObject::Symbol& symbol)
{
    if (symbol.is_undefined() || symbol.bind() == STB_WEAK)
        return DynamicLinker::lookup_global_symbol(symbol.name());

    return DynamicObject::SymbolLookupResult { symbol.value(), symbol.size(), symbol.address(), symbol.bind(), &symbol.object() };
}

} // end namespace ELF
