#include <AK/Platform.h>

ENTRY(init)

#define PF_X 0x1
#define PF_W 0x2
#define PF_R 0x4

PHDRS
{
  elf_headers PT_LOAD FILEHDR PHDRS FLAGS(PF_R) ;
  super_pages PT_LOAD FLAGS(PF_R | PF_W) ;
  text PT_LOAD FLAGS(PF_R | PF_X) ;
  data PT_LOAD FLAGS(PF_R | PF_W) ;
  bss PT_LOAD FLAGS(PF_R | PF_W) ;
  dynamic_segment PT_LOAD FLAGS(PF_R | PF_W) ;
  dynamic PT_DYNAMIC FLAGS(PF_R | PF_W) ;
  ksyms PT_LOAD FLAGS(PF_R) ;
}

SECTIONS
{
    start_of_kernel_image = .;

    .elf_headers (SIZEOF_HEADERS) : AT (ADDR(.elf_headers) + SIZEOF_HEADERS)
    {
        start_of_elf_headers = .;
    } :elf_headers

    .super_pages ALIGN(4K) (NOLOAD) : AT (ADDR(.super_pages))
    {
        *(.super_pages)
    } :super_pages

    .text ALIGN(4K) : AT (ADDR(.text))
    {
        start_of_kernel_text = .;

        start_of_safemem_text = .;
        KEEP(*(.text.safemem))
        end_of_safemem_text = .;
        start_of_safemem_atomic_text = .;
        KEEP(*(.text.safemem.atomic))
        end_of_safemem_atomic_text = .;

        *(.text*)
    } :text

    .unmap_after_init ALIGN(4K) : AT (ADDR(.unmap_after_init))
    {
        start_of_unmap_after_init = .;
        *(.unmap_after_init*);
        end_of_unmap_after_init = .;

        end_of_kernel_text = .;
    } :text

    .rodata ALIGN(4K) : AT (ADDR(.rodata))
    {
        start_heap_ctors = .;
        *libkernel_heap.a:*(.ctors)
        end_heap_ctors = .;

        start_ctors = .;
        *(.ctors)
        end_ctors = .;

        *(.rodata*)
    } :data

    .data ALIGN(4K) : AT (ADDR(.data))
    {
        start_of_kernel_data = .;
        *(.data*)
        end_of_kernel_data = .;
    } :data

    .ro_after_init ALIGN(4K) (NOLOAD) : AT(ADDR(.ro_after_init))
    {
        start_of_ro_after_init = .;
        *(.ro_after_init);
        end_of_ro_after_init = .;
    } :data

    .bss ALIGN(4K) (NOLOAD) : AT (ADDR(.bss))
    {
        start_of_kernel_bss = .;
        *(page_tables)
        *(COMMON)
        *(.bss*)
        end_of_kernel_bss = .;

        . = ALIGN(4K);
        *(.heap)
    } :bss

    .dynamic ALIGN(4K) : AT (ADDR(.dynamic))
    {
        *(.dynamic)
    } :dynamic_segment :dynamic

    .ksyms ALIGN(4K) : AT (ADDR(.ksyms))
    {
        start_of_kernel_ksyms = .;
        *(.kernel_symbols)
        end_of_kernel_ksyms = .;
    } :ksyms

    end_of_kernel_image = .;
}
