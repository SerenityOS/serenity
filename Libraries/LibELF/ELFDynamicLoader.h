/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibELF/ELFDynamicObject.h>
#include <LibELF/ELFImage.h>
#include <LibELF/exec_elf.h>
#include <mman.h>

#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

#define ALIGN_ROUND_UP(x, align) ((((size_t)(x)) + align - 1) & (~(align - 1)))

class ELFDynamicLoader : public RefCounted<ELFDynamicLoader> {
public:
    static NonnullRefPtr<ELFDynamicLoader> construct(const char* filename, int fd, size_t file_size);

    ~ELFDynamicLoader();

    bool is_valid() const { return m_valid; }

    // Load a full ELF image from file into the current process and create an ELFDynamicObject
    // from the SHT_DYNAMIC in the file.
    bool load_from_image(unsigned flags);

    // Stage 2 of loading: relocations and init functions
    // Assumes that the program headers have been loaded and that m_dynamic_object is initialized
    // Splitting loading like this allows us to use the same code to relocate a main executable as an elf binary
    bool load_stage_2(unsigned flags);

    // Intended for use by dlsym or other internal methods
    void* symbol_for_name(const char*);

    void dump();

    // Will be called from _fixup_plt_entry, as part of the PLT trampoline
    Elf32_Addr patch_plt_entry(u32 relocation_offset);

private:
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

        u32 required_load_size() { return ALIGN_ROUND_UP(m_program_header.p_memsz, m_program_header.p_align); }

    private:
        Elf32_Phdr m_program_header; // Explictly a copy of the PHDR in the image
    };

    explicit ELFDynamicLoader(const char* filename, int fd, size_t file_size);
    explicit ELFDynamicLoader(Elf32_Dyn* dynamic_location, Elf32_Addr load_address);

    // Stage 1
    void load_program_headers(const ELFImage& elf_image);

    // Stage 2
    void do_relocations();
    void setup_plt_trampoline();
    void call_object_init_functions();

    String m_filename;
    size_t m_file_size { 0 };
    int m_image_fd { -1 };
    void* m_file_mapping { nullptr };
    bool m_valid { true };

    OwnPtr<ELFDynamicObject> m_dynamic_object;

    VirtualAddress m_text_segment_load_address;
    size_t m_text_segment_size;

    VirtualAddress m_tls_segment_address;
    VirtualAddress m_dynamic_section_address;
};
