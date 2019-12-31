#pragma once

#include <LibELF/ELFImage.h>
#include <LibELF/exec_elf.h>
#include <dlfcn.h>
#include <mman.h>

#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

#define ALIGN_ROUND_UP(x, align) ((((size_t)(x)) + align - 1) & (~(align - 1)))

class ELFDynamicObject : public RefCounted<ELFDynamicObject> {
public:
    static NonnullRefPtr<ELFDynamicObject> construct(const char* filename, int fd, size_t file_size);

    ~ELFDynamicObject();

    bool is_valid() const { return m_valid; }

    // FIXME: How can we resolve all of the symbols without having the original elf image for our process?
    //     RTLD_LAZY only at first probably... though variables ('objects') need resolved at load time every time
    bool load(unsigned flags);

    // Intended for use by dlsym or other internal methods
    void* symbol_for_name(const char*);

    void dump();

private:
    class ProgramHeaderRegion {
    public:
        ProgramHeaderRegion(const Elf32_Phdr& header)
            : m_program_header(header)
        {
        }

        VirtualAddress load_address() const { return m_load_address; }
        VirtualAddress base_address() const { return m_image_base_address; }

        void set_load_address(VirtualAddress addr) { m_load_address = addr; }
        void set_base_address(VirtualAddress addr) { m_image_base_address = addr; }

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
        VirtualAddress m_load_address { 0 };
        VirtualAddress m_image_base_address { 0 };
    };

    explicit ELFDynamicObject(const char* filename, int fd, size_t file_size);

    String m_filename;
    size_t m_file_size { 0 };
    int m_image_fd { -1 };
    void* m_file_mapping { nullptr };
    bool m_valid { false };

    OwnPtr<ELFImage> m_image;

    void parse_dynamic_section();
    void do_relocations();

    static void patch_plt_entry(u32 got_offset, void* dso_got_tag);

    Vector<ProgramHeaderRegion> m_program_header_regions;
    ProgramHeaderRegion* m_text_region { nullptr };
    ProgramHeaderRegion* m_data_region { nullptr };
    ProgramHeaderRegion* m_tls_region { nullptr };

    // Begin Section information collected from DT_* entries
    uintptr_t m_init_offset { 0 };
    uintptr_t m_fini_offset { 0 };

    uintptr_t m_init_array_offset { 0 };
    size_t m_init_array_size { 0 };

    uintptr_t m_hash_table_offset { 0 };

    uintptr_t m_string_table_offset { 0 };
    uintptr_t m_symbol_table_offset { 0 };
    size_t m_size_of_string_table { 0 };
    size_t m_size_of_symbol_table_entry { 0 };

    Elf32_Sword m_procedure_linkage_table_relocation_type { -1 };
    uintptr_t m_plt_relocation_offset_location { 0 }; // offset of PLT relocations, at end of relocations
    size_t m_size_of_plt_relocation_entry_list { 0 };
    uintptr_t m_procedure_linkage_table_offset { 0 };

    // NOTE: We'll only ever either RELA or REL entries, not both (thank god)
    size_t m_number_of_relocations { 0 };
    size_t m_size_of_relocation_entry { 0 };
    size_t m_size_of_relocation_table { 0 };
    uintptr_t m_relocation_table_offset { 0 };

    // DT_FLAGS
    bool m_should_process_origin = false;
    bool m_requires_symbolic_symbol_resolution = false;
    // Text relocations meaning: we need to edit the .text section which is normally mapped PROT_READ
    bool m_has_text_relocations = false;
    bool m_must_bind_now = false; // FIXME: control with an environment var as well?
    bool m_has_static_thread_local_storage = false;
    // End Section information from DT_* entries
};
