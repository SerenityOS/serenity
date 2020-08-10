ENTRY(start)

SECTIONS
{
    . = 0xc0100000;

    start_of_kernel_image = .;

    .text ALIGN(4K) : AT (ADDR(.text) - 0xc0000000)
    {
        $<TARGET_OBJECTS:boot>
        *(.multiboot)
        start_of_kernel_text = .;
        *(.text)
        *(.text.startup)
        end_of_kernel_text = .;
    }

    .rodata ALIGN(4K) : AT (ADDR(.rodata) - 0xc0000000)
    {
        start_heap_ctors = .;
        *libkernel_heap.a:*(.ctors)
        end_heap_ctors = .;

        start_ctors = .;
        *(.ctors)
        end_ctors = .;

        *(.rodata)
    }

    .data ALIGN(4K) : AT (ADDR(.data) - 0xc0000000)
    {
        start_of_kernel_data = .;
        *(.data)
        end_of_kernel_data = .;
    }

    .bss ALIGN(4K) : AT (ADDR(.bss) - 0xc0000000)
    {
        start_of_kernel_bss = .;
        *(page_tables)
        *(COMMON)
        *(.bss)
        end_of_kernel_bss = .;
    }

    end_of_kernel_image = .;
}
