/*
 * Copyright (c) 2023, Sönke Holz <sholz830@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

ENTRY(init)

#define PF_X 0x1
#define PF_W 0x2
#define PF_R 0x4

PHDRS
{
    text PT_LOAD FLAGS(PF_R | PF_X);
    data PT_LOAD FLAGS(PF_R | PF_W);
    bss PT_LOAD FLAGS(PF_R | PF_W);
    dynamic_segment PT_LOAD FLAGS(PF_R | PF_W);
    dynamic PT_DYNAMIC FLAGS(PF_R | PF_W);
    ksyms PT_LOAD FLAGS(PF_R | PF_W);
}

SECTIONS
{
    start_of_kernel_image = .;

    .text ALIGN(4K) :
    {
        start_of_kernel_text = .;
        *(.text.first)

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

    .unmap_after_init ALIGN(4K) :
    {
        start_of_unmap_after_init = .;
        *(.unmap_after_init*);
        end_of_unmap_after_init = .;

        end_of_kernel_text = .;
    } :text

    .rodata ALIGN(4K) :
    {
        start_heap_ctors = .;
        *libkernel_heap.a:*(.init_array)
        end_heap_ctors = .;

        start_ctors = .;
        *(.init_array)
        end_ctors = .;

        *(.srodata* .rodata*)
    } :data

    .data ALIGN(4K) :
    {
        start_of_kernel_data = .;
        *(.sdata* .data*)
        end_of_kernel_data = .;
    } :data

    .ro_after_init ALIGN(4K) :
    {
        start_of_ro_after_init = .;
        *(.ro_after_init);
        end_of_ro_after_init = .;
    } :data

    /* The bss has to be in its own program header so the prekernel doesn't have to copy segments */
    .bss ALIGN(4K) (NOLOAD) :
    {
        start_of_bss = .;
        *(.sbss* .bss*)
        end_of_bss = .;

        . = ALIGN(4K);
        *(.heap)
    } :bss

    .dynamic ALIGN(4K) :
    {
        *(.dynamic)
    } :dynamic_segment :dynamic

    .ksyms ALIGN(4K) :
    {
        start_of_kernel_ksyms = .;
        *(.kernel_symbols)
        end_of_kernel_ksyms = .;
    } :ksyms

    . = ALIGN(4K);
    start_of_initial_stack = .;

    . += 32K;
    end_of_initial_stack = .;

    /*
        FIXME: 8MB is enough space for all of the tables required to identity map
               physical memory. 8M is wasteful, so this should be properly calculated.
    */

    . = ALIGN(4K);
    page_tables_phys_start = .;

    . += 8M;
    page_tables_phys_end = .;

    end_of_kernel_image = .;
}
