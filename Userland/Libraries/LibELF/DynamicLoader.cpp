/*
 * Copyright (c) 2019-2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibELF/Arch/GenericDynamicRelocationType.h>
#include <LibELF/Arch/tls.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/Hashes.h>
#include <LibELF/Validation.h>
#include <assert.h>
#include <bits/dlfcn_integration.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifndef AK_OS_SERENITY
static void* mmap_with_name(void* addr, size_t length, int prot, int flags, int fd, off_t offset, char const*)
{
    return mmap(addr, length, prot, flags, fd, offset);
}

#    define MAP_RANDOMIZED 0
#endif

#if ARCH(AARCH64)
#    define HAS_TLSDESC_SUPPORT
extern "C" {
void* __tlsdesc_static(void*);
}
#endif

namespace ELF {

Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> DynamicLoader::try_create(int fd, ByteString filepath)
{
    VERIFY(filepath.starts_with('/'));

    struct stat stat;
    if (fstat(fd, &stat) < 0) {
        return DlErrorMessage { "DynamicLoader::try_create fstat" };
    }

    VERIFY(stat.st_size >= 0);
    auto size = static_cast<size_t>(stat.st_size);
    if (size < sizeof(Elf_Ehdr))
        return DlErrorMessage { ByteString::formatted("File {} has invalid ELF header", filepath) };

    ByteString file_mmap_name = ByteString::formatted("ELF_DYN: {}", filepath);
    auto* data = mmap_with_name(nullptr, size, PROT_READ, MAP_SHARED, fd, 0, file_mmap_name.characters());
    if (data == MAP_FAILED) {
        return DlErrorMessage { "DynamicLoader::try_create mmap" };
    }

    auto loader = adopt_ref(*new DynamicLoader(fd, move(filepath), data, size));
    if (!loader->is_valid())
        return DlErrorMessage { "ELF image validation failed" };
    return loader;
}

DynamicLoader::DynamicLoader(int fd, ByteString filepath, void* data, size_t size)
    : m_filepath(move(filepath))
    , m_file_size(size)
    , m_image_fd(fd)
    , m_file_data(data)
{
    m_elf_image = adopt_own(*new ELF::Image((u8*)m_file_data, m_file_size));
    m_valid = validate();
    if (m_valid)
        find_tls_size_and_alignment();
    else
        dbgln("Image validation failed for file {}", m_filepath);
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

void DynamicLoader::find_tls_size_and_alignment()
{
    image().for_each_program_header([this](auto program_header) {
        if (program_header.type() == PT_TLS) {
            m_tls_size_of_current_object = program_header.size_in_memory();
            auto alignment = program_header.alignment();
            VERIFY(!alignment || is_power_of_two(alignment));
            m_tls_alignment_of_current_object = alignment > 1 ? alignment : 0; // No need to reserve extra space for single byte alignment
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
}

bool DynamicLoader::validate()
{
    if (!image().is_valid())
        return false;

    auto* elf_header = (Elf_Ehdr*)m_file_data;
    if (!validate_elf_header(*elf_header, m_file_size))
        return false;
    [[maybe_unused]] Optional<Elf_Phdr> interpreter_path_program_header {};
    return validate_program_headers(*elf_header, m_file_size, { m_file_data, m_file_size }, interpreter_path_program_header);
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

    m_dynamic_object = DynamicObject::create(m_filepath, m_base_address, m_dynamic_section_address);
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
        dbgln("\033[33mWarning:\033[0m Dynamic object {} has text relocations", m_dynamic_object->filepath());
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
    } else {
        // .text needs to be executable while we process relocations because it might contain IFUNC resolvers.
        // We don't allow IFUNC resolvers in objects with textrels.
        for (auto& text_segment : m_text_segments) {
            if (mprotect(text_segment.address().as_ptr(), text_segment.size(), PROT_READ | PROT_EXEC) < 0) {
                perror("mprotect .text: PROT_READ | PROT_EXEC");
                return false;
            }
        }
    }
    do_main_relocations();
    return true;
}

void DynamicLoader::do_main_relocations()
{
    do_relr_relocations();

    Optional<DynamicLoader::CachedLookupResult> cached_result;
    m_dynamic_object->relocation_section().for_each_relocation([&](DynamicObject::Relocation const& relocation) {
        switch (do_direct_relocation(relocation, cached_result, ShouldCallIfuncResolver::No)) {
        case RelocationResult::Failed:
            dbgln("Loader.so: {} unresolved symbol '{}'", m_filepath, relocation.symbol().name());
            VERIFY_NOT_REACHED();
        case RelocationResult::CallIfuncResolver:
            m_direct_ifunc_relocations.append(relocation);
            break;
        case RelocationResult::Success:
            break;
        }
    });

    // If the object is position-independent, the pointer to the PLT trampoline needs to be relocated.
    auto fixup_trampoline_pointer = [&](DynamicObject::Relocation const& relocation) {
        VERIFY(static_cast<GenericDynamicRelocationType>(relocation.type()) == GenericDynamicRelocationType::JUMP_SLOT);
        if (image().is_dynamic())
            *((FlatPtr*)relocation.address().as_ptr()) += m_dynamic_object->base_address().get();
    };

    m_dynamic_object->plt_relocation_section().for_each_relocation([&](DynamicObject::Relocation const& relocation) {
        if (static_cast<GenericDynamicRelocationType>(relocation.type()) == GenericDynamicRelocationType::IRELATIVE) {
            m_direct_ifunc_relocations.append(relocation);
            return;
        }
        if (static_cast<GenericDynamicRelocationType>(relocation.type()) == GenericDynamicRelocationType::TLSDESC) {
            // GNU ld for some reason puts TLSDESC relocations into .rela.plt
            // https://sourceware.org/bugzilla/show_bug.cgi?id=28387
            auto result = do_direct_relocation(relocation, cached_result, ShouldCallIfuncResolver::No);
            VERIFY(result == RelocationResult::Success);
            return;
        }

        // FIXME: Or LD_BIND_NOW is set?
        if (m_dynamic_object->must_bind_now()) {
            switch (do_plt_relocation(relocation, ShouldCallIfuncResolver::No)) {
            case RelocationResult::Failed:
                dbgln("Loader.so: {} unresolved symbol '{}'", m_filepath, relocation.symbol().name());
                VERIFY_NOT_REACHED();
            case RelocationResult::CallIfuncResolver:
                m_plt_ifunc_relocations.append(relocation);
                // Set up lazy binding, in case an IFUNC resolver calls another IFUNC that hasn't been resolved yet.
                fixup_trampoline_pointer(relocation);
                break;
            case RelocationResult::Success:
                break;
            }
        } else {
            fixup_trampoline_pointer(relocation);
        }
    });
}

Result<NonnullRefPtr<DynamicObject>, DlErrorMessage> DynamicLoader::load_stage_3(unsigned flags)
{
    if (flags & RTLD_LAZY) {
        if (m_dynamic_object->has_plt())
            setup_plt_trampoline();
    }

    // IFUNC resolvers can only be called after the PLT has been populated,
    // as they may call arbitrary functions via the PLT.
    for (auto const& relocation : m_plt_ifunc_relocations) {
        auto result = do_plt_relocation(relocation, ShouldCallIfuncResolver::Yes);
        VERIFY(result == RelocationResult::Success);
    }

    Optional<DynamicLoader::CachedLookupResult> cached_result;
    for (auto const& relocation : m_direct_ifunc_relocations) {
        auto result = do_direct_relocation(relocation, cached_result, ShouldCallIfuncResolver::Yes);
        VERIFY(result == RelocationResult::Success);
    }

    if (m_dynamic_object->has_text_relocations()) {
        // If we don't have textrels, .text has already been made executable by this point in load_stage_2.
        for (auto& text_segment : m_text_segments) {
            if (mprotect(text_segment.address().as_ptr(), text_segment.size(), PROT_READ | PROT_EXEC) < 0) {
                return DlErrorMessage { ByteString::formatted("mprotect .text: PROT_READ | PROT_EXEC: {}", strerror(errno)) };
            }
        }
    }

    if (m_relro_segment_size) {
        if (mprotect(m_relro_segment_address.as_ptr(), m_relro_segment_size, PROT_READ) < 0) {
            return DlErrorMessage { ByteString::formatted("mprotect .relro: PROT_READ: {}", strerror(errno)) };
        }

#ifdef AK_OS_SERENITY
        if (set_mmap_name(m_relro_segment_address.as_ptr(), m_relro_segment_size, ByteString::formatted("{}: .relro", m_filepath).characters()) < 0) {
            return DlErrorMessage { ByteString::formatted("set_mmap_name .relro: {}", strerror(errno)) };
        }
#endif
    }

    m_fully_relocated = true;

    return NonnullRefPtr<DynamicObject> { *m_dynamic_object };
}

void DynamicLoader::load_stage_4()
{
    call_object_init_functions();

    m_fully_initialized = true;
}

void DynamicLoader::load_program_headers()
{
    FlatPtr ph_load_start = SIZE_MAX;
    FlatPtr ph_load_end = 0;

    // We walk the program header list once to find the requested address ranges of the program.
    // We don't fill in the list of regions yet to keep malloc memory blocks from interfering with our reservation.
    image().for_each_program_header([&](Image::ProgramHeader const& program_header) {
        if (program_header.type() != PT_LOAD)
            return;

        FlatPtr section_start = program_header.vaddr().get();
        FlatPtr section_end = section_start + program_header.size_in_memory();

        if (ph_load_start > section_start)
            ph_load_start = section_start;

        if (ph_load_end < section_end)
            ph_load_end = section_end;
    });

    void* requested_load_address = image().is_dynamic() ? nullptr : reinterpret_cast<void*>(ph_load_start);

    int reservation_mmap_flags = MAP_ANON | MAP_PRIVATE | MAP_NORESERVE;
    if (image().is_dynamic())
        reservation_mmap_flags |= MAP_RANDOMIZED;
#ifdef MAP_FIXED_NOREPLACE
    else
        reservation_mmap_flags |= MAP_FIXED_NOREPLACE;
#endif

    // First, we make a dummy reservation mapping, in order to allocate enough VM
    // to hold all regions contiguously in the address space.

    FlatPtr ph_load_base = ph_load_start & ~(FlatPtr)0xfffu;
    ph_load_end = round_up_to_power_of_two(ph_load_end, PAGE_SIZE);

    size_t total_mapping_size = ph_load_end - ph_load_base;

    // Before we make our reservation, unmap our existing mapped ELF image that we used for reading header information.
    // This leaves our pointers dangling momentarily, but it reduces the chance that we will conflict with ourselves.
    if (munmap(m_file_data, m_file_size) < 0) {
        perror("munmap old mapping");
        VERIFY_NOT_REACHED();
    }
    m_elf_image = nullptr;
    m_file_data = nullptr;

    auto* reservation = mmap(requested_load_address, total_mapping_size, PROT_NONE, reservation_mmap_flags, 0, 0);
    if (reservation == MAP_FAILED) {
        perror("mmap reservation");
        VERIFY_NOT_REACHED();
    }

    // Now that we can't accidentally block our requested space, re-map our ELF image.
    ByteString file_mmap_name = ByteString::formatted("ELF_DYN: {}", m_filepath);
    auto* data = mmap_with_name(nullptr, m_file_size, PROT_READ, MAP_SHARED, m_image_fd, 0, file_mmap_name.characters());
    if (data == MAP_FAILED) {
        perror("mmap new mapping");
        VERIFY_NOT_REACHED();
    }

    m_file_data = data;
    m_elf_image = adopt_own(*new ELF::Image((u8*)m_file_data, m_file_size));

    VERIFY(requested_load_address == nullptr || reservation == requested_load_address);

    m_base_address = VirtualAddress { reservation };

    // Most binaries have four loadable regions, three of which are mapped
    // (symbol tables/relocation information, executable instructions, read-only data)
    // and one of which is copied (modifiable data).
    // These are allocated in-line to cut down on the malloc calls.
    Vector<ProgramHeaderRegion, 3> map_regions;
    Vector<ProgramHeaderRegion, 1> copy_regions;
    Optional<ProgramHeaderRegion> relro_region;

    VirtualAddress dynamic_region_desired_vaddr;

    image().for_each_program_header([&](Image::ProgramHeader const& program_header) {
        ProgramHeaderRegion region {};
        region.set_program_header(program_header.raw_header());
        if (region.is_tls_template()) {
            // Skip, this is handled in DynamicLoader::copy_initial_tls_data_into.
        } else if (region.is_load()) {
            if (region.size_in_memory() == 0)
                return;
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

    quick_sort(map_regions, compare_load_address);
    quick_sort(copy_regions, compare_load_address);

    // Pre-allocate any malloc memory needed before unmapping the reservation.
    // We don't want any future malloc to accidentally mmap a reserved address!
    ByteString text_segment_name = ByteString::formatted("{}: .text", m_filepath);
    ByteString rodata_segment_name = ByteString::formatted("{}: .rodata", m_filepath);
    ByteString data_segment_name = ByteString::formatted("{}: .data", m_filepath);

    m_text_segments.ensure_capacity(map_regions.size());

    // Finally, we unmap the reservation.
    if (munmap(reservation, total_mapping_size) < 0) {
        perror("munmap reservation");
        VERIFY_NOT_REACHED();
    }

    // WARNING: Allocating after this point has the possibility of malloc stealing our reserved
    // virtual memory addresses. Be careful not to malloc below!

    // Process regions in order: .text, .data, .tls
    for (auto& region : map_regions) {
        FlatPtr ph_desired_base = region.desired_load_address().get();
        FlatPtr ph_base = region.desired_load_address().page_base().get();
        FlatPtr ph_end = ph_base + round_up_to_power_of_two(region.size_in_memory() + region.desired_load_address().get() - ph_base, PAGE_SIZE);

        char const* const segment_name = region.is_executable() ? text_segment_name.characters() : rodata_segment_name.characters();

        // Now we can map the text segment at the reserved address.
        auto* segment_base = (u8*)mmap_with_name(
            (u8*)reservation + ph_base - ph_load_base,
            ph_desired_base - ph_base + region.size_in_image(),
            PROT_READ,
            MAP_SHARED | MAP_FIXED,
            m_image_fd,
            VirtualAddress { region.offset() }.page_base().get(),
            segment_name);

        if (segment_base == MAP_FAILED) {
            perror("mmap non-writable");
            VERIFY_NOT_REACHED();
        }

        // NOTE: Capacity ensured above the line of no malloc above
        if (region.is_executable())
            m_text_segments.unchecked_append({ VirtualAddress { segment_base }, ph_end - ph_base });
    }

    VERIFY(requested_load_address == nullptr || requested_load_address == reservation);

    if (relro_region.has_value()) {
        m_relro_segment_size = relro_region->size_in_memory();
        m_relro_segment_address = VirtualAddress { (u8*)reservation + relro_region->desired_load_address().get() - ph_load_base };
    }

    if (image().is_dynamic())
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
            data_segment_name.characters());

        if (MAP_FAILED == data_segment) {
            perror("mmap writable");
            VERIFY_NOT_REACHED();
        }

        VirtualAddress data_segment_start;
        if (image().is_dynamic())
            data_segment_start = VirtualAddress { (u8*)reservation + region.desired_load_address().get() };
        else
            data_segment_start = region.desired_load_address();

        VERIFY(data_segment_start.as_ptr() + region.size_in_memory() <= data_segment + data_segment_size);

        memcpy(data_segment_start.as_ptr(), (u8*)m_file_data + region.offset(), region.size_in_image());
    }
}

DynamicLoader::RelocationResult DynamicLoader::do_direct_relocation(DynamicObject::Relocation const& relocation,
    Optional<DynamicLoader::CachedLookupResult>& cached_result,
    ShouldCallIfuncResolver should_call_ifunc_resolver)
{
    FlatPtr* patch_ptr = nullptr;
    if (is_dynamic())
        patch_ptr = (FlatPtr*)(m_dynamic_object->base_address().as_ptr() + relocation.offset());
    else
        patch_ptr = (FlatPtr*)(FlatPtr)relocation.offset();

    auto call_ifunc_resolver = [](VirtualAddress address) {
        return VirtualAddress { reinterpret_cast<DynamicObject::IfuncResolver>(address.get())() };
    };

    auto lookup_symbol = [&](DynamicObject::Symbol const& symbol) {
        // The static linker sorts relocations by the referenced symbol. Especially when vtables
        // in large inheritance hierarchies are involved, there might be tens of references to
        // the same symbol. We can avoid redundant lookups by keeping track of the previous result.
        if (!cached_result.has_value() || !cached_result.value().symbol.definitely_equals(symbol))
            cached_result = DynamicLoader::CachedLookupResult { symbol, DynamicLoader::lookup_symbol(symbol) };
        return cached_result.value().result;
    };

    struct ResolvedTLSSymbol {
        DynamicObject const& dynamic_object;
        FlatPtr value;
    };

    auto resolve_tls_symbol = [&](DynamicObject::Relocation const& relocation) -> Optional<ResolvedTLSSymbol> {
        if (relocation.symbol_index() == 0)
            return ResolvedTLSSymbol { relocation.dynamic_object(), 0 };

        auto res = lookup_symbol(relocation.symbol());
        if (!res.has_value())
            return {};
        VERIFY(relocation.symbol().type() != STT_GNU_IFUNC);
        VERIFY(res.value().dynamic_object != nullptr);
        return ResolvedTLSSymbol { *res.value().dynamic_object, res.value().value };
    };

    using enum GenericDynamicRelocationType;
    switch (static_cast<GenericDynamicRelocationType>(relocation.type())) {

    case NONE:
        // Apparently most loaders will just skip these?
        // Seems if the 'link editor' generates one something is funky with your code
        break;
    case ABSOLUTE: {
        auto symbol = relocation.symbol();
        auto res = lookup_symbol(symbol);
        VirtualAddress symbol_address;
        if (!res.has_value()) {
            if (symbol.bind() != STB_WEAK) {
                dbgln("ERROR: symbol not found: {}.", symbol.name());
                return RelocationResult::Failed;
            }
            symbol_address = VirtualAddress { (FlatPtr)0 };
        } else {
            if (res.value().type == STT_GNU_IFUNC && should_call_ifunc_resolver == ShouldCallIfuncResolver::No)
                return RelocationResult::CallIfuncResolver;
            symbol_address = res.value().address;
        }
        if (relocation.addend_used())
            *patch_ptr = symbol_address.get() + relocation.addend();
        else
            *patch_ptr += symbol_address.get();
        if (res.has_value() && res.value().type == STT_GNU_IFUNC)
            *patch_ptr = call_ifunc_resolver(VirtualAddress { *patch_ptr }).get();
        break;
    }
#if !ARCH(RISCV64)
    case GLOB_DAT: {
        auto symbol = relocation.symbol();
        auto res = lookup_symbol(symbol);
        VirtualAddress symbol_location;
        if (!res.has_value()) {
            if (symbol.bind() != STB_WEAK) {
                // Symbol not found
                return RelocationResult::Failed;
            }

            symbol_location = VirtualAddress { (FlatPtr)0 };
        } else {
            symbol_location = res.value().address;
            if (res.value().type == STT_GNU_IFUNC) {
                if (should_call_ifunc_resolver == ShouldCallIfuncResolver::No)
                    return RelocationResult::CallIfuncResolver;
                if (res.value().dynamic_object != nullptr && res.value().dynamic_object->has_text_relocations()) {
                    dbgln("\033[31mError:\033[0m Refusing to call IFUNC resolver defined in an object with text relocations.");
                    return RelocationResult::Failed;
                }
                symbol_location = call_ifunc_resolver(symbol_location);
            }
        }
        VERIFY(symbol_location != m_dynamic_object->base_address());
        *patch_ptr = symbol_location.get();
        break;
    }
#endif
    case RELATIVE: {
        if (!image().is_dynamic())
            break;
        // FIXME: According to the spec, R_386_relative ones must be done first.
        //     We could explicitly do them first using m_number_of_relocations from DT_RELCOUNT
        //     However, our compiler is nice enough to put them at the front of the relocations for us :)
        if (relocation.addend_used())
            *patch_ptr = m_dynamic_object->base_address().offset(relocation.addend()).get();
        else
            *patch_ptr += m_dynamic_object->base_address().get();
        break;
    }
    case TLS_TPREL: {
        auto maybe_resolution = resolve_tls_symbol(relocation);
        if (!maybe_resolution.has_value())
            break;
        auto [dynamic_object_of_symbol, symbol_value] = maybe_resolution.value();

        size_t addend = relocation.addend_used() ? relocation.addend() : *patch_ptr;
        *patch_ptr = addend + dynamic_object_of_symbol.tls_offset().value() + symbol_value + TLS_TP_STATIC_TLS_BLOCK_OFFSET;

        if constexpr (TLS_VARIANT == 1) {
            // Until offset TLS_TP_STATIC_TLS_BLOCK_OFFSET there's the thread's ThreadControlBlock, we don't want to collide with it.
            VERIFY(static_cast<ssize_t>(*patch_ptr) >= static_cast<ssize_t>(TLS_TP_STATIC_TLS_BLOCK_OFFSET));
        } else if constexpr (TLS_VARIANT == 2) {
            // At offset 0 there's the thread's ThreadControlBlock, we don't want to collide with it.
            VERIFY(static_cast<ssize_t>(*patch_ptr) < 0);
        }

        break;
    }
    case TLS_DTPMOD: {
        auto maybe_resolution = resolve_tls_symbol(relocation);
        if (!maybe_resolution.has_value())
            break;

        // We repurpose the module index to store the TLS block's TP offset. This is fine
        // because we currently only support a single static TLS block.
        *patch_ptr = maybe_resolution->dynamic_object.tls_offset().value();
        break;
    }
    case TLS_DTPREL: {
        auto maybe_resolution = resolve_tls_symbol(relocation);
        if (!maybe_resolution.has_value())
            break;

        size_t addend = relocation.addend_used() ? relocation.addend() : *patch_ptr;
        *patch_ptr = addend + maybe_resolution->value - TLS_DTV_OFFSET + TLS_TP_STATIC_TLS_BLOCK_OFFSET;
        break;
    }
#ifdef HAS_TLSDESC_SUPPORT
    case TLSDESC: {
        auto maybe_resolution = resolve_tls_symbol(relocation);
        if (!maybe_resolution.has_value())
            break;
        auto [dynamic_object_of_symbol, symbol_value] = maybe_resolution.value();

        size_t addend = relocation.addend_used() ? relocation.addend() : *patch_ptr;

        patch_ptr[0] = (FlatPtr)__tlsdesc_static;
        patch_ptr[1] = addend + dynamic_object_of_symbol.tls_offset().value() + symbol_value;
        break;
    }
#endif
    case IRELATIVE: {
        if (should_call_ifunc_resolver == ShouldCallIfuncResolver::No)
            return RelocationResult::CallIfuncResolver;
        VirtualAddress resolver;
        if (relocation.addend_used())
            resolver = m_dynamic_object->base_address().offset(relocation.addend());
        else
            resolver = m_dynamic_object->base_address().offset(*patch_ptr);

        if (m_dynamic_object->has_text_relocations()) {
            dbgln("\033[31mError:\033[0m Refusing to call IFUNC resolver defined in an object with text relocations.");
            return RelocationResult::Failed;
        }

        *patch_ptr = call_ifunc_resolver(resolver).get();
        break;
    }
    case JUMP_SLOT:
        VERIFY_NOT_REACHED(); // PLT relocations are handled by do_plt_relocation.
    default:
        // Raise the alarm! Someone needs to implement this relocation type
        dbgln("Found a new exciting relocation type {}", relocation.type());
        VERIFY_NOT_REACHED();
    }
    return RelocationResult::Success;
}

DynamicLoader::RelocationResult DynamicLoader::do_plt_relocation(DynamicObject::Relocation const& relocation, ShouldCallIfuncResolver should_call_ifunc_resolver)
{
    VERIFY(static_cast<GenericDynamicRelocationType>(relocation.type()) == GenericDynamicRelocationType::JUMP_SLOT);
    auto symbol = relocation.symbol();
    auto* relocation_address = (FlatPtr*)relocation.address().as_ptr();

    VirtualAddress symbol_location {};
    if (auto result = lookup_symbol(symbol); result.has_value()) {
        auto address = result.value().address;

        if (result.value().type == STT_GNU_IFUNC) {
            if (should_call_ifunc_resolver == ShouldCallIfuncResolver::No)
                return RelocationResult::CallIfuncResolver;
// FIXME: IFUNC resolvers do not actually return an ElfAddr aka an int,
//        But a pointer to a function. UBSan doesn't like us lying about that.
//        This seems to only be detected on Clang
//        To temporarily disable UBsan an IIFE is needed, as sanitizers aren't diagnostics...
#ifdef AK_COMPILER_CLANG
            [&] [[clang::no_sanitize("undefined")]] {
                symbol_location = VirtualAddress { reinterpret_cast<DynamicObject::IfuncResolver>(address.get())() };
            }();
#else
            symbol_location = VirtualAddress { reinterpret_cast<DynamicObject::IfuncResolver>(address.get())() };
#endif
        } else {
            symbol_location = address;
        }
    } else if (symbol.bind() != STB_WEAK) {
        return RelocationResult::Failed;
    }

    dbgln_if(DYNAMIC_LOAD_DEBUG, "DynamicLoader: Jump slot relocation: putting {} ({}) into PLT at {}", symbol.name(), symbol_location, (void*)relocation_address);
    *relocation_address = symbol_location.get();

    return RelocationResult::Success;
}

void DynamicLoader::do_relr_relocations()
{
    auto base_address = m_dynamic_object->base_address().get();
    m_dynamic_object->for_each_relr_relocation([base_address](FlatPtr address) {
        *(FlatPtr*)address += base_address;
    });
}

void DynamicLoader::copy_initial_tls_data_into(Bytes buffer) const
{
    image().for_each_program_header([this, &buffer](ELF::Image::ProgramHeader program_header) {
        if (program_header.type() != PT_TLS)
            return IterationDecision::Continue;

        // Note: The "size in image" is only concerned with initialized data. Uninitialized data (.tbss) is
        // only included in the "size in memory" metric, and is expected to not be touched or read from, as
        // it is not present in the image and zeroed out in-memory. We will still check that the buffer has
        // space for both the initialized and the uninitialized data.
        // TODO: Is the initialized data always in the beginning of the TLS segment, or should we walk the
        // sections to figure that out?

        VERIFY(program_header.size_in_image() <= program_header.size_in_memory());
        VERIFY(program_header.size_in_memory() <= m_tls_size_of_current_object);

        if constexpr (TLS_VARIANT == 1) {
            size_t tls_start_in_buffer = m_tls_offset;
            VERIFY(tls_start_in_buffer + program_header.size_in_memory() <= buffer.size());
            memcpy(buffer.data() + tls_start_in_buffer, static_cast<u8 const*>(m_file_data) + program_header.offset(), program_header.size_in_image());
        } else if constexpr (TLS_VARIANT == 2) {
            size_t tls_start_in_buffer = buffer.size() + m_tls_offset;
            VERIFY(tls_start_in_buffer + program_header.size_in_memory() <= buffer.size());
            memcpy(buffer.data() + tls_start_in_buffer, static_cast<u8 const*>(m_file_data) + program_header.offset(), program_header.size_in_image());
        }

        return IterationDecision::Break;
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

#if ARCH(AARCH64) || ARCH(X86_64)
    got_ptr[1] = (FlatPtr)m_dynamic_object.ptr();
    got_ptr[2] = (FlatPtr)&_plt_trampoline;
#elif ARCH(RISCV64)
    got_ptr[0] = (FlatPtr)&_plt_trampoline;
    got_ptr[1] = (FlatPtr)m_dynamic_object.ptr();
#else
#    error Unknown architecture
#endif
}

// Called from our ASM routine _plt_trampoline.
extern "C" FlatPtr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset)
{
    auto const& relocation = object->plt_relocation_section().relocation_at_offset(relocation_offset);
    auto result = DynamicLoader::do_plt_relocation(relocation, ShouldCallIfuncResolver::Yes);
    if (result != DynamicLoader::RelocationResult::Success) {
        dbgln("Loader.so: {} unresolved symbol '{}'", object->filepath(), relocation.symbol().name());
        VERIFY_NOT_REACHED();
    }
    return *reinterpret_cast<FlatPtr*>(relocation.address().as_ptr());
}

void DynamicLoader::call_object_init_functions()
{
    typedef void (*InitFunc)();

    if (m_dynamic_object->has_init_section()) {
        auto init_function = m_dynamic_object->init_section_function();
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

    return DynamicObject::SymbolLookupResult { symbol.value(), symbol.size(), symbol.address(), symbol.bind(), symbol.type(), &symbol.object() };
}

void DynamicLoader::compute_topological_order(Vector<NonnullRefPtr<DynamicLoader>>& topological_order)
{
    VERIFY(m_topological_ordering_state == TopologicalOrderingState::NotVisited);
    m_topological_ordering_state = TopologicalOrderingState::Visiting;

    Vector<NonnullRefPtr<DynamicLoader>> actual_dependencies;
    for (auto const& dependency : m_true_dependencies) {
        auto state = dependency->m_topological_ordering_state;
        if (state == TopologicalOrderingState::NotVisited)
            dependency->compute_topological_order(topological_order);
        if (state == TopologicalOrderingState::Visited)
            actual_dependencies.append(dependency);
    }
    m_true_dependencies = actual_dependencies;

    m_topological_ordering_state = TopologicalOrderingState::Visited;
    topological_order.append(*this);
}

}
