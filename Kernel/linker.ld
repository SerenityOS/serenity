ENTRY(start)

SECTIONS
{
    . = 0x100000;

    .text BLOCK(4K) : ALIGN(4K)
    {
        Arch/i386/Boot/boot.ao
        *(.multiboot)
        *(.page_tables)
        start_of_kernel_text = .;
        *(.text)
        *(.text.startup)
        end_of_kernel_text = .;
    }

    .rodata BLOCK(4K) : ALIGN(4K)
    {
        start_ctors = .;
        *(.ctors)
        end_ctors = .;

        *(.rodata)
    }

    .data BLOCK(4K) : ALIGN(4K)
    {
        start_of_kernel_data = .;
        *(.data)
        end_of_kernel_data = .;
    }

    .bss BLOCK(4K) : ALIGN(4K)
    {
        start_of_kernel_bss = .;
        *(COMMON)
        *(.bss)
        end_of_kernel_bss = .;
    }
}
