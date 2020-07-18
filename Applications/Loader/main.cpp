/*
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

#include <LibELF/AuxiliaryData.h>

#include "DynamicObject.h"
#include "Heap.h"
#include "LibC/limits.h"
#include "Syscalls.h"
#include "Utils.h"

uint8_t loaded_libs_buffer[sizeof(List<DynamicObject>)];
List<DynamicObject>* loaded_libs_list = nullptr;

u8* g_tls_pool_start = 0;
u8* g_tls_pool_end = 0;
size_t g_tls_pool_size = 0;
// size_t g_tls_pool_size = 4;

u32 read_gs_0()
{
    u32 cr3;
    asm("movl %%gs:0x0, %%eax"
        : "=a"(cr3));
    return cr3;
}

u32 read_gs_0_addr()
{
    u32 cr3;
    asm("leal %%gs:0x0, %%eax"
        : "=a"(cr3));
    return cr3;
}

void write_gs_0(u32 value)
{
    asm("movl %0, %%gs:0x0"
        :
        : "r"(value));
}

bool is_library_loaded(const char* lib_name)
{
    for (const auto loaded_lib : *loaded_libs_list) {
        if (!strcmp(lib_name, loaded_lib.object_name())) {
            return true;
        }
    }
    return false;
}

ELF::AuxiliaryData load_library(int fd, const char* library_name, const Elf32_Ehdr* elf_header)
{
    ASSERT(elf_header->e_type == ET_DYN);
    const uint8_t* image_base = (const uint8_t*)elf_header;

    const Elf32_Phdr* text_header = nullptr;
    const Elf32_Phdr* data_header = nullptr;
    const Elf32_Phdr* dynamic_header = nullptr;
    const Elf32_Phdr* tls_header = nullptr;

    for (size_t program_header_index = 0; program_header_index < elf_header->e_phnum; ++program_header_index) {
        const Elf32_Phdr* program_header = ((const Elf32_Phdr*)(image_base + elf_header->e_phoff)) + program_header_index;
        if (program_header->p_type == PT_LOAD) {
            if (program_header->p_flags & PF_X) {
                text_header = program_header;
            } else {
                ASSERT(program_header->p_flags & (PF_R | PF_W));
            }
            data_header = program_header;
        } else if (program_header->p_type == PT_DYNAMIC) {
            dynamic_header = program_header;
        } else if (program_header->p_type == PT_TLS) {
            tls_header = program_header;
        }
    }

    ASSERT(text_header);
    ASSERT(data_header);
    ASSERT(dynamic_header);

    char text_section_name[256];
    // TODO: snprintf
    sprintf(text_section_name, "%s - text", library_name);

    void* base_address = serenity_mmap(
        nullptr,
        text_header->p_memsz,
        PROT_READ | PROT_EXEC,
        MAP_PRIVATE,
        fd,
        text_header->p_offset,
        text_header->p_align,
        text_section_name);

    ASSERT(base_address);

    u32 text_segment_size = ALIGN_ROUND_UP(text_header->p_memsz, text_header->p_align);

    // TODO: use this
    // void* dynamic_section_address = dynamic_header.value().vaddr().offset((u32)m_base_address).as_ptr();

    char data_section_name[256];
    // TODO: snprintf
    sprintf(data_section_name, "%s - data", library_name);

    void* data_segment_begin = serenity_mmap(
        (void*)((u32)base_address + text_segment_size),
        data_header->p_memsz,
        PROT_READ | PROT_WRITE,
        MAP_ANONYMOUS | MAP_PRIVATE,
        0, // no fd
        0, // 0 offset
        data_header->p_align,
        data_section_name);

    ASSERT(data_segment_begin);

    uint8_t* data_segment_actual_addr = (uint8_t*)(data_header->p_vaddr + ((u32)base_address));
    memcpy(data_segment_actual_addr, (const u8*)elf_header + data_header->p_offset, data_header->p_filesz);

    ELF::AuxiliaryData aux;
    aux.program_headers = (u32)base_address + elf_header->e_phoff;
    aux.num_program_headers = elf_header->e_phnum;
    aux.entry_point = elf_header->e_entry + (u32)base_address;
    aux.base_address = (u32)base_address;
    aux.text_segment_size = text_header->p_memsz;
    if (tls_header) {
        aux.tls_section_size = tls_header->p_memsz;
        u8* current_tls_block_start = g_tls_pool_start + g_tls_pool_size;
        // TODO: This is not entirely correct.
        // we should first figure out how much TLS we need, allocate it, and only then copy to it.
        // In the current way of doing things we are limited to the single page of TLS we currently get.
        memcpy(current_tls_block_start, (const u8*)elf_header + tls_header->p_offset, tls_header->p_filesz);
    }
    return aux;
}

ELF::AuxiliaryData load_library(const char* library_name)
{
    constexpr size_t MAX_LIBNAME = 256;
    char library_path[MAX_LIBNAME] = "/usr/lib/";
    // TODO: snprintf
    sprintf(library_path, "/usr/lib/%s", library_name);

    dbgprintf("loading: %s\n", library_path);
    int fd = open(library_path, O_RDONLY);
    ASSERT(fd >= 0);
    struct stat lib_stat;
    int rc = fstat(fd, &lib_stat);
    ASSERT(!rc);

    void* mmap_result = serenity_mmap(nullptr, lib_stat.st_size, PROT_READ, MAP_PRIVATE, fd, 0, PAGE_SIZE, library_path);
    ASSERT(mmap_result != nullptr);

    const Elf32_Ehdr* elf_header = (Elf32_Ehdr*)(mmap_result);

    auto result = load_library(fd, library_name, elf_header);

    munmap(mmap_result, lib_stat.st_size);
    close(fd);

    return result;
}

struct SymbolLookupResult {
    bool found { false };
    Elf32_Addr value { 0 };
    Elf32_Addr address { 0 };
    const DynamicObject* dynamic_object { nullptr }; // The object in which the symbol is defined
};

SymbolLookupResult global_symbol_lookup(const char* symbol_name)
{
    for (auto& object : *loaded_libs_list) {
        auto res = object.lookup_symbol(symbol_name);
        if (res.is_undefined())
            continue;
        return { true, res.value(), res.address(), &object };
    }
    return {};
}

SymbolLookupResult lookup_symbol(const DynamicObject::Symbol& symbol)
{
    if (!symbol.is_undefined()) {
        return { true, symbol.value(), symbol.address(), &symbol.object() };
    }
    VERBOSE("looking up symbol: %s\n", symbol.name());
    return global_symbol_lookup(symbol.name());
}

void do_relocations(DynamicObject& dynamic_object)
{
    dynamic_object.for_each_relocation([&](DynamicObject::Relocation relocation) {
        VERBOSE("Relocation symbol: %s, type: %d\n", relocation.symbol().name(), relocation.type());
        u32* patch_ptr = (u32*)(dynamic_object.base_address() + relocation.offset());
        VERBOSE("dynamic object name: %s\n", dynamic_object.object_name());
        VERBOSE("dynamic object base address: %p\n", dynamic_object.base_address());
        VERBOSE("relocation offset: 0x%x\n", relocation.offset());
        VERBOSE("patch_ptr: %p\n", patch_ptr);
        switch (relocation.type()) {
        case R_386_NONE:
            // Apparently most loaders will just skip these?
            // Seems if the 'link editor' generates one something is funky with your code
            VERBOSE("None relocation. No symbol, no nothin.\n");
            break;
        case R_386_32: {
            auto symbol = relocation.symbol();
            VERBOSE("Absolute relocation: name: '%s', value: %p\n", symbol.name(), symbol.value());
            auto res = lookup_symbol(symbol);
            ASSERT(res.found);
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
            u32 relative_offset = (res.address - (relocation.offset() + dynamic_object.base_address()));
            *patch_ptr += relative_offset;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_GLOB_DAT: {
            auto symbol = relocation.symbol();
            VERBOSE("Global data relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            auto res = lookup_symbol(symbol);
            if (!res.found) {
                // TODO this is a hack
                ASSERT(!strcmp(symbol.name(), "__deregister_frame_info") || !strcmp(symbol.name(), "_ITM_registerTMCloneTable")
                    || !strcmp(symbol.name(), "_ITM_deregisterTMCloneTable") || !strcmp(symbol.name(), "__register_frame_info"));
                // ASSERT_NOT_REACHED();
                return IterationDecision::Continue;
            }
            // ASSERT(res.found);
            u32 symbol_location = res.address;
            ASSERT(symbol_location != dynamic_object.base_address());
            *patch_ptr = symbol_location;
            VERBOSE("   Symbol address: %p\n", *patch_ptr);
            break;
        }
        case R_386_RELATIVE: {
            // FIXME: According to the spec, R_386_relative ones must be done first.
            //     We could explicitly do them first using m_number_of_relocatoins from DT_RELCOUNT
            //     However, our compiler is nice enough to put them at the front of the relocations for us :)
            VERBOSE("Load address relocation at offset %X\n", relocation.offset());
            VERBOSE("    patch ptr == %p, adding load base address (%p) to it and storing %p\n", *patch_ptr, dynamic_object.base_address(), *patch_ptr + dynamic_object.base_address());
            *patch_ptr += dynamic_object.base_address(); // + addend for RelA (addend for Rel is stored at addr)
            break;
        }
        case R_386_TLS_TPOFF: {
            VERBOSE("Relocation type: R_386_TLS_TPOFF at offset %X\n", relocation.offset());
            auto symbol = relocation.symbol();
            VERBOSE("Symbol index: %d\n", symbol.index());
            VERBOSE("Symbol is_undefined?: %d\n", symbol.is_undefined());
            VERBOSE("TLS relocation: '%s', value: %p\n", symbol.name(), symbol.value());
            auto res = lookup_symbol(symbol);
            if (!res.found)
                break;
            ASSERT(res.found);
            u32 symbol_value = res.value;
            VERBOSE("symbol value: %d\n", symbol_value);
            *patch_ptr = (res.dynamic_object->tls_end_offset() - g_tls_pool_size - symbol_value - 4);
            VERBOSE("*patch ptr: %d\n", (i32)*patch_ptr);
            break;
        }
        case R_386_TLS_DTPMOD3: {
            ASSERT_NOT_REACHED();
            // // FIXME
            // dbg() << "TODO: Offset in TLS block";
            // ASSERT_NOT_REACHED();
            // break;
        }
        case R_386_TLS_DTPOFF32: {
            ASSERT_NOT_REACHED();
            // // FIXME
            // dbg() << "TOODO: R_386_TLS_DTPOFF32";
            // ASSERT_NOT_REACHED();
            // break;
        }
        default:
            // Raise the alarm! Someone needs to implement this relocation type
            VERBOSE("Found a new exciting relocation type %d\n", relocation.type());
            // printf("DynamicLoader: Found unknown relocation type %d\n", relocation.type());
            ASSERT_NOT_REACHED();
            break;
        }
        return IterationDecision::Continue;
    });
}

void do_plt_relocations(DynamicObject& dynamic_object)
{
    (void)dynamic_object;
    dynamic_object.for_each_plt_relocation([&](DynamicObject::Relocation relocation) {
        ASSERT(relocation.type() == R_386_JMP_SLOT);

        auto sym = relocation.symbol();

        u8* relocation_address = (u8*)relocation.address();
        auto res = lookup_symbol(sym);
        ASSERT(res.found);
        u32 symbol_location = res.address;

        VERBOSE("DynamicLoader: Jump slot relocation: putting %s (%p) into PLT at %p\n", sym.name(), symbol_location, relocation_address);

        *(u32*)relocation_address = symbol_location;
    });
}

void call_init_functions(DynamicObject& dynamic_object)
{
    if (dynamic_object.has_init_section()) {
        VERBOSE("init_section_function: %p\n", dynamic_object.init_section_function());
        dynamic_object.init_section_function()();
    }
    dynamic_object.for_each_initialization_array_function([](DynamicObject::InitializationFunction func) {
        VERBOSE("jumping to initialization function: %p\n", func);
        func();
    });
}

void load_dependencies(const ELF::AuxiliaryData& aux_data)
{
    VERBOSE("entry point: %p\n", aux_data.entry_point);
    VERBOSE("program headers: %p\n", aux_data.program_headers);
    VERBOSE("num program headers: %p\n", aux_data.num_program_headers);
    VERBOSE("base address: %p\n", aux_data.base_address);

    Elf32_Addr dynamic_section_addr = 0;
    for (size_t i = 0; i < aux_data.num_program_headers; ++i) {
        Elf32_Phdr* phdr = &((Elf32_Phdr*)aux_data.program_headers)[i];
        VERBOSE("phdr: %p\n", phdr);
        VERBOSE("phdr type: %d\n", phdr->p_type);
        if (phdr->p_type == PT_DYNAMIC) {
            dynamic_section_addr = aux_data.base_address + phdr->p_offset;
        }
    }

    if (!dynamic_section_addr)
        exit(1);

    DynamicObject* dynamic_object = new DynamicObject(aux_data);
    g_tls_pool_size += dynamic_object->tls_size();
    dynamic_object->set_tls_end_offset(g_tls_pool_size);

    for (const auto needed_library : (*dynamic_object).needed_libraries()) {
        if (!is_library_loaded(needed_library)) {
            auto dependency_aux_data = load_library(needed_library);
            load_dependencies(dependency_aux_data);
        }
    }

    loaded_libs_list->append(*dynamic_object);
}

void handle_loaded_object(DynamicObject& dynamic_object)
{
    VERBOSE("handle_loaded_object: %s\n", dynamic_object.object_name());

    if (dynamic_object.has_text_relocations()) {
        int rc = mprotect(reinterpret_cast<void*>(dynamic_object.text_segment_load_address()),
            dynamic_object.text_segment_size(),
            PROT_READ | PROT_WRITE);
        ASSERT(rc >= 0);
    }

    do_relocations(dynamic_object);
    do_plt_relocations(dynamic_object);

    if (dynamic_object.has_text_relocations()) {
        int rc = mprotect(reinterpret_cast<void*>(dynamic_object.text_segment_load_address()),
            dynamic_object.text_segment_size(),
            PROT_READ | PROT_EXEC);
        ASSERT(rc >= 0);
    }

    VERBOSE("calling init functions\n");
    call_init_functions(dynamic_object);
}

void initialize_tls_pool()
{
    // dbgprintf("gs[0] before: 0x%x\n", read_gs_0());
    // for (auto& loaded_library : *loaded_libs_list) {
    //     dbgprintf("lib: %s, tls size: %d\n", loaded_library.object_name(), loaded_library.tls_size());
    // }
    g_tls_pool_end = g_tls_pool_start + g_tls_pool_size;
    write_gs_0((u32)g_tls_pool_end);
    // dbgprintf("tls size: %d\n", tls_pool_size);
    // dbgprintf("gs[0] initialized to: 0x%x\n", read_gs_0());
}

int main(int x, char**)
{

    // dbgprintf("&gs[0]: 0x%x\n", read_gs_0_addr());
    g_tls_pool_start = (u8*)read_gs_0() + 4;
    // g_tls_pool_start = (u8*)read_gs_0();

    new (loaded_libs_buffer)(List<char*>)();
    loaded_libs_list = reinterpret_cast<List<DynamicObject>*>(loaded_libs_buffer);
    ELF::AuxiliaryData* aux_data = (ELF::AuxiliaryData*)x;
    load_dependencies(*aux_data);

    initialize_tls_pool();

    for (auto& loaded_library : *loaded_libs_list) {
        handle_loaded_object(loaded_library);
    }

    dbgprintf("jumping to entry point: %p\n", aux_data->base_address);
    dbgprintf("entrypoint[0] = 0x%x\n", *((uint32_t*)aux_data->entry_point));

    volatile uint32_t x_addr = (uint32_t)&x;
    uint32_t* sp = (uint32_t*)(x_addr);
    size_t argc = *(size_t*)(sp + 4);
    char** argv = *(char***)(sp + 5);
    char** envp = *(char***)(sp + 6);
    dbgprintf("sp: %p\n", sp);
    dbgprintf("argc: %d\n", argc);
    dbgprintf("argv: %p\n", argv);
    dbgprintf("envp: %p\n", envp);

    int result = ((int (*)(int, char**, char**))(aux_data->entry_point))(argc, argv, envp);

    dbgprintf("result: %d\n", result);

    // hang();

    exit(0);
    return 0;
}
