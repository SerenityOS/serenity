ENTRY(init)

#define PF_X 0x1
#define PF_W 0x2
#define PF_R 0x4

PHDRS
{
    elf_headers PT_LOAD FILEHDR PHDRS FLAGS(PF_R) ;
    text PT_LOAD FLAGS(PF_R | PF_X) ;
    data PT_LOAD FLAGS(PF_R | PF_W) ;
    bss PT_LOAD FLAGS(PF_R | PF_W) ;
    dynamic_segment PT_LOAD FLAGS(PF_R | PF_W) ;
    dynamic PT_DYNAMIC FLAGS(PF_R | PF_W) ;
    ksyms PT_LOAD FLAGS(PF_R | PF_W) ;
}

SECTIONS
{
    start_of_kernel_image = .;

    .elf_headers (SIZEOF_HEADERS) : AT (ADDR(.elf_headers) + SIZEOF_HEADERS)
    {
        start_of_elf_headers = .;
    } :elf_headers

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

    .driver_init ALIGN(4K) : AT (ADDR(.driver_init))
    {
        driver_init_table_start = .;
        *(.driver_init)
        driver_init_table_end = .;
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
        *libkernel_heap.a:*(.init_array)
        end_heap_ctors = .;

        start_ctors = .;
        *(.ctors)
        *(.init_array)
        end_ctors = .;

        *(.rodata*)
    } :data

    .data ALIGN(4K) : AT (ADDR(.data))
    {
        start_of_kernel_data = .;
        *(.data*)
        end_of_kernel_data = .;
    } :data

    .ro_after_init ALIGN(4K) : AT(ADDR(.ro_after_init))
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
