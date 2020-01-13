// HACK ALERT: We make the printf impl here use our strlen instead of the one in libc (which is linked in)
//     So that we can use it before relocations are done
#define strlen priv_strlen
#include <AK/PrintfImplementation.h>
#undef strlen

#include <LibELF/exec_elf.h>
#include <Kernel/VM/VirtualAddress.h>
#include <Kernel/Syscall.h>

#include <LibELF/ELFDynamicObject.h>
#include <AK/Assertions.h>

typedef int (*MainFunction)(int, char**, char**);

extern "C" {

int priv_dbgprintf(const char* fmt, ...);

static const Elf32_Rel* last_processed_rel = 0;

void ld_elf_relocate_self(Elf32_Dyn* dynamic_section, Elf32_Addr self_base_addr)
{
    // We're going to relocate just the R_386_RELATIVE entries
    // After that, we can pass control back to the assembly to jump into ld_elf_main
    // and that'll do the rest of the setup. Including setting up our global offset table relocation pointers, etc

    priv_dbgprintf("DYN ADDR: %p, SELF_BASE_ADDR: %p\n", dynamic_section, self_base_addr);

    VirtualAddress self_base(self_base_addr);
    const Elf32_Rel* relocations_start = nullptr;
    size_t initial_relocations_length = 0;
    const Elf32_Dyn* start_of_dyn = dynamic_section;

    for (; start_of_dyn->d_tag != DT_NULL; ++start_of_dyn) {
        switch(start_of_dyn->d_tag) {
            case DT_REL:
                relocations_start = (Elf32_Rel*)(self_base.offset(start_of_dyn->d_un.d_ptr).as_ptr());
                priv_dbgprintf("DT_REL: d_ptr: 0x%p. relocations_start: %p\n", self_base.offset(start_of_dyn->d_un.d_ptr), relocations_start);
                break;
            case DT_RELSZ:
                initial_relocations_length = start_of_dyn->d_un.d_val;
                priv_dbgprintf("DT_RELSZ: d_val: %zu\n", start_of_dyn->d_un.d_ptr);
                break;
        }
    }

    const Elf32_Rel* relocations_end = &relocations_start[initial_relocations_length/sizeof(Elf32_Rel)];
    u8* load_base_address = self_base.as_ptr();
    priv_dbgprintf("RELOCATIONS START: 0x%p, END: 0x%p\n", relocations_start, relocations_end);

    for (; relocations_start != relocations_end; ++relocations_start) {
        u32 relocation_type = ELF32_R_TYPE(relocations_start->r_info);
        u32 relocation_offset = relocations_start->r_offset;
        u32* patch_ptr = (u32*)(load_base_address + relocation_offset);
    
        switch(relocation_type) {
        case R_386_RELATIVE:
            priv_dbgprintf("RELATIVE RELOCATION AT 0x%p\n", relocation_offset);
            *patch_ptr += self_base.get();
            break;
        default:
            // We can't actually process any symbol based ones here yet...
            // If we have other relocation types in this executable we did something wrong, probably
            last_processed_rel = relocations_start;
            priv_dbgprintf("Unexpected relocation type %zu\n", relocation_type);
            break;
        }
    }

    priv_dbgprintf("RELOCATIONS START: %p END: %p\n", relocations_end - initial_relocations_length, relocations_end);
}

size_t priv_strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

int priv_dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal([](char*&, char ch) { syscall(SC_dbgputch, ch); }, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

int hang_main(int argc, char** argv, char** envp)
{
    priv_dbgprintf("ARGC: %d, ARGV: 0x%p, ENVP: 0x%p", argc, argv, envp);
    syscall(SC_dbgputstr, "\nHANG MAIN:\n", 12);
    for (int i = 0; i < argc; ++i) {
        syscall(SC_dbgputstr, argv[i], priv_strlen(argv[i]));
        syscall(SC_dbgputch, '\n'); 
    }
    for (char** envvar = envp; *envvar; ++envvar) {
        syscall(SC_dbgputstr, *envvar, priv_strlen(*envvar));
        syscall(SC_dbgputch, '\n'); 
    }
    syscall(SC_dbgputstr, "Aight, I'mma head out\n", 23);
    syscall(SC_sleep, 2);
    CRASH();
    return 0;
}

const char* g_main_program_name;

MainFunction ld_elf_main(Elf32_Addr* stack_ptr, Elf32_Addr self_base_address)
{
    priv_dbgprintf("STACK PTR: %p, SELF BASE: %p\n", stack_ptr, self_base_address);
    priv_dbgprintf("ARGC: %d, ARGV: 0x%p, ENVP: 0x%p\n", stack_ptr[0], stack_ptr[1], stack_ptr[2]);

    if (!stack_ptr[0]) {
        priv_dbgprintf("Woah there, there's no program name on my stack! Cya later!\n");
        CRASH();
    }
    g_main_program_name = ((char**)(stack_ptr[1]))[0];
    char** local_environ = (char**)stack_ptr[2];

    priv_dbgprintf("environ: %p, main_program_name: %s\n", local_environ, g_main_program_name);
    return hang_main;
}

}


