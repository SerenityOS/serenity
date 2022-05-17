/*
 * Copyright (c) 2023, Sönke Holz <sholz830@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

ENTRY(start)

KERNEL_MAPPING_BASE = 0x2000000000;

/* TODO: Add FLAGS to the program headers */
PHDRS
{
    text PT_LOAD ;
    data PT_LOAD ;
    ksyms PT_LOAD ;
    bss PT_LOAD ;
}

SECTIONS
{
    . = KERNEL_MAPPING_BASE;

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
        end_of_kernel_text = .;
    } :text

    .driver_init ALIGN(4K) : AT (ADDR(.driver_init))
    {
        driver_init_table_start = .;
        *(.driver_init)
        driver_init_table_end = .;
    } :text

    .rodata ALIGN(4K) :
    {
        start_heap_ctors = .;
        *libkernel_heap.a:*(.init_array)
        end_heap_ctors = .;

        start_ctors = .;
        *(.init_array)
        end_ctors = .;

        *(.rodata*)
    } :data

    .data ALIGN(4K) :
    {
        start_of_kernel_data = .;
        *(.data*)
        end_of_kernel_data = .;
    } :data

    .ksyms ALIGN(4K) :
    {
        start_of_kernel_ksyms = .;
        *(.kernel_symbols)
        end_of_kernel_ksyms = .;
    } :ksyms

    .bss ALIGN(4K) (NOLOAD) :
    {
        start_of_bss = .;
        *(.bss)
        end_of_bss = .;

        . = ALIGN(4K);
        *(.heap)
    } :bss

    /*
        FIXME: 8MB is enough space for all of the tables required to identity map
               physical memory. 8M is wasteful, so this should be properly calculated.
    */

    /* FIXME: Placeholder to satisfy linker */
    start_of_unmap_after_init = .;
    end_of_unmap_after_init = .;
    start_of_ro_after_init = .;
    end_of_ro_after_init = .;

    . = ALIGN(4K);
    page_tables_phys_start = .;

    . += 8M;
    page_tables_phys_end = .;

    end_of_kernel_image = .;
}
