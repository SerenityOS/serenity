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

#pragma once

#include <AK/Assertions.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/exec_elf.h>
#include <sys/mman.h>

namespace ELF {

#define ALIGN_ROUND_UP(x, align) ((((size_t)(x)) + align - 1) & (~(align - 1)))

class DynamicLoader : public RefCounted<DynamicLoader> {
public:
    static RefPtr<DynamicLoader> try_create(int fd, String filename);
    ~DynamicLoader();

    bool is_valid() const { return m_valid; }

    // Load a full ELF image from file into the current process and create an DynamicObject
    // from the SHT_DYNAMIC in the file.
    // Note that the DynamicObject will not be linked yet. Callers are responsible for calling link() to finish it.
    RefPtr<DynamicObject> map();

    bool link(unsigned flags, size_t total_tls_size);

    // Stage 2 of loading: dynamic object loading and primary relocations
    bool load_stage_2(unsigned flags, size_t total_tls_size);

    // Stage 3 of loading: lazy relocations and initializers
    RefPtr<DynamicObject> load_stage_3(unsigned flags, size_t total_tls_size);
    // Intended for use by dlsym or other internal methods
    void* symbol_for_name(const char*);

    void dump();

    // Requested program interpreter from program headers. May be empty string
    StringView program_interpreter() const { return m_program_interpreter; }

    VirtualAddress text_segment_load_addresss() const { return m_text_segment_load_address; }

    void set_tls_offset(size_t offset) { m_tls_offset = offset; };
    size_t tls_size() const { return m_tls_size; }
    size_t tls_offset() const { return m_tls_offset; }
    const ELF::Image& image() const { return m_elf_image; }

    template<typename F>
    void for_each_needed_library(F) const;

    VirtualAddress text_segment_load_address() const { return m_text_segment_load_address; }
    bool is_dynamic() const { return m_elf_image.is_dynamic(); }

private:
    DynamicLoader(int fd, String filename, void* file_data, size_t file_size);

    class ProgramHeaderRegion {
    public:
        void set_program_header(const Elf32_Phdr& header) { m_program_header = header; }

        // Information from ELF Program header
        u32 type() const { return m_program_header.p_type; }
        u32 flags() const { return m_program_header.p_flags; }
        u32 offset() const { return m_program_header.p_offset; }
        VirtualAddress desired_load_address() const { return VirtualAddress(m_program_header.p_vaddr); }
        u32 size_in_memory() const { return m_program_header.p_memsz; }
        u32 size_in_image() const { return m_program_header.p_filesz; }
        u32 alignment() const { return m_program_header.p_align; }
        u32 mmap_prot() const;
        bool is_readable() const { return flags() & PF_R; }
        bool is_writable() const { return flags() & PF_W; }
        bool is_executable() const { return flags() & PF_X; }
        bool is_tls_template() const { return type() == PT_TLS; }
        bool is_load() const { return type() == PT_LOAD; }
        bool is_dynamic() const { return type() == PT_DYNAMIC; }
        bool is_relro() const { return type() == PT_GNU_RELRO; }

    private:
        Elf32_Phdr m_program_header; // Explicitly a copy of the PHDR in the image
    };

    const DynamicObject& dynamic_object() const;

    // Stage 1
    void load_program_headers();

    // Stage 2
    void do_main_relocations(size_t total_tls_size);

    // Stage 3
    void do_lazy_relocations(size_t total_tls_size);
    void setup_plt_trampoline();
    void call_object_init_functions();

    bool validate();

    enum class RelocationResult : uint8_t {
        Failed = 0,
        Success = 1,
        ResolveLater = 2,
    };
    RelocationResult do_relocation(size_t total_tls_size, DynamicObject::Relocation relocation);
    size_t calculate_tls_size() const;

    Optional<DynamicObject::SymbolLookupResult> lookup_symbol(const ELF::DynamicObject::Symbol&) const;

    String m_filename;
    String m_program_interpreter;
    size_t m_file_size { 0 };
    int m_image_fd { -1 };
    void* m_file_data { nullptr };
    ELF::Image m_elf_image;
    bool m_valid { true };

    RefPtr<DynamicObject> m_dynamic_object;

    VirtualAddress m_text_segment_load_address;
    size_t m_text_segment_size { 0 };

    VirtualAddress m_relro_segment_address;
    size_t m_relro_segment_size { 0 };

    VirtualAddress m_tls_segment_address;
    VirtualAddress m_dynamic_section_address;

    size_t m_tls_offset { 0 };
    size_t m_tls_size { 0 };

    Vector<DynamicObject::Relocation> m_unresolved_relocations;

    mutable RefPtr<DynamicObject> m_cached_dynamic_object;
};

template<typename F>
void DynamicLoader::for_each_needed_library(F func) const
{
    dynamic_object().for_each_needed_library(move(func));
}

} // end namespace ELF
