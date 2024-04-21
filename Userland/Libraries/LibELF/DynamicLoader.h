/*
 * Copyright (c) 2019-2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/ELFABI.h>
#include <LibELF/Image.h>
#include <bits/dlfcn_integration.h>
#include <sys/mman.h>

namespace ELF {

class LoadedSegment {
public:
    LoadedSegment(VirtualAddress address, size_t size)
        : m_address(address)
        , m_size(size)
    {
    }

    VirtualAddress address() const { return m_address; }
    size_t size() const { return m_size; }

private:
    VirtualAddress m_address;
    size_t m_size;
};

enum class ShouldCallIfuncResolver {
    Yes,
    No
};

extern "C" FlatPtr _fixup_plt_entry(DynamicObject* object, u32 relocation_offset);

class DynamicLoader : public RefCounted<DynamicLoader> {
public:
    static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> try_create(int fd, ByteString filepath);
    ~DynamicLoader();

    ByteString const& filepath() const { return m_filepath; }

    bool is_valid() const { return m_valid; }

    // Load a full ELF image from file into the current process and create an DynamicObject
    // from the SHT_DYNAMIC in the file.
    // Note that the DynamicObject will not be linked yet. Callers are responsible for calling link() to finish it.
    RefPtr<DynamicObject> map();

    bool link(unsigned flags);

    // Stage 2 of loading: dynamic object loading and primary relocations
    bool load_stage_2(unsigned flags);

    // Stage 3 of loading: lazy relocations
    Result<NonnullRefPtr<DynamicObject>, DlErrorMessage> load_stage_3(unsigned flags);

    // Stage 4 of loading: initializers
    void load_stage_4();

    void set_tls_offset(size_t offset) { m_tls_offset = offset; }
    size_t tls_size_of_current_object() const { return m_tls_size_of_current_object; }
    size_t tls_alignment_of_current_object() const { return m_tls_alignment_of_current_object; }
    size_t tls_offset() const { return m_tls_offset; }
    const ELF::Image& image() const { return *m_elf_image; }

    template<typename F>
    void for_each_needed_library(F) const;

    VirtualAddress base_address() const { return m_base_address; }
    Vector<LoadedSegment> const text_segments() const { return m_text_segments; }
    bool is_dynamic() const { return image().is_dynamic(); }

    static Optional<DynamicObject::SymbolLookupResult> lookup_symbol(const ELF::DynamicObject::Symbol&);
    void copy_initial_tls_data_into(Bytes buffer) const;

    DynamicObject& dynamic_object() { return *m_dynamic_object; }
    DynamicObject const& dynamic_object() const { return *m_dynamic_object; }

    bool is_fully_relocated() const { return m_fully_relocated; }
    bool is_fully_initialized() const { return m_fully_initialized; }

    void add_dependency(NonnullRefPtr<DynamicLoader> dependency)
    {
        // Dependencies that aren't actually true will be removed in compute_topological_order.
        m_true_dependencies.append(move(dependency));
    }

    void compute_topological_order(Vector<NonnullRefPtr<DynamicLoader>>& topological_order);

private:
    DynamicLoader(int fd, ByteString filepath, void* file_data, size_t file_size);

    class ProgramHeaderRegion {
    public:
        void set_program_header(Elf_Phdr const& header) { m_program_header = header; }

        // Information from ELF Program header
        u32 type() const { return m_program_header.p_type; }
        u32 flags() const { return m_program_header.p_flags; }
        u32 offset() const { return m_program_header.p_offset; }
        VirtualAddress desired_load_address() const { return VirtualAddress(m_program_header.p_vaddr); }
        u32 size_in_memory() const { return m_program_header.p_memsz; }
        u32 size_in_image() const { return m_program_header.p_filesz; }
        u32 alignment() const { return m_program_header.p_align; }
        bool is_readable() const { return flags() & PF_R; }
        bool is_writable() const { return flags() & PF_W; }
        bool is_executable() const { return flags() & PF_X; }
        bool is_tls_template() const { return type() == PT_TLS; }
        bool is_load() const { return type() == PT_LOAD; }
        bool is_dynamic() const { return type() == PT_DYNAMIC; }
        bool is_relro() const { return type() == PT_GNU_RELRO; }

    private:
        Elf_Phdr m_program_header; // Explicitly a copy of the PHDR in the image
    };

    // Stage 1
    void load_program_headers();

    // Stage 2
    void do_main_relocations();

    // Stage 3
    void setup_plt_trampoline();

    // Stage 4
    void call_object_init_functions();

    bool validate();

    friend FlatPtr _fixup_plt_entry(DynamicObject*, u32);

    enum class RelocationResult : uint8_t {
        Failed,
        Success,
        CallIfuncResolver,
    };
    struct CachedLookupResult {
        DynamicObject::Symbol symbol;
        Optional<DynamicObject::SymbolLookupResult> result;
    };
    RelocationResult do_direct_relocation(DynamicObject::Relocation const&, Optional<CachedLookupResult>&, ShouldCallIfuncResolver);
    // Will be called from _fixup_plt_entry, as part of the PLT trampoline
    static RelocationResult do_plt_relocation(DynamicObject::Relocation const&, ShouldCallIfuncResolver);
    void do_relr_relocations();
    void find_tls_size_and_alignment();

    ByteString m_filepath;
    size_t m_file_size { 0 };
    int m_image_fd { -1 };
    void* m_file_data { nullptr };
    OwnPtr<ELF::Image> m_elf_image;
    bool m_valid { true };

    RefPtr<DynamicObject> m_dynamic_object;

    VirtualAddress m_base_address;
    Vector<LoadedSegment> m_text_segments;

    VirtualAddress m_relro_segment_address;
    size_t m_relro_segment_size { 0 };

    VirtualAddress m_dynamic_section_address;

    ssize_t m_tls_offset { 0 };
    size_t m_tls_size_of_current_object { 0 };
    size_t m_tls_alignment_of_current_object { 0 };

    Vector<DynamicObject::Relocation> m_direct_ifunc_relocations;
    Vector<DynamicObject::Relocation> m_plt_ifunc_relocations;

    bool m_fully_relocated { false };
    bool m_fully_initialized { false };

    enum class TopologicalOrderingState {
        NotVisited,
        Visiting,
        Visited,
    } m_topological_ordering_state { TopologicalOrderingState::NotVisited };

    Vector<NonnullRefPtr<DynamicLoader>> m_true_dependencies;
};

template<typename F>
void DynamicLoader::for_each_needed_library(F func) const
{
    dynamic_object().for_each_needed_library(move(func));
}

} // end namespace ELF
