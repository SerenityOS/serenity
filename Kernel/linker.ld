ENTRY(start)

SECTIONS
{
	. = 0xc0100000;

	.text ALIGN(4K) : AT(ADDR(.text) - 0xc0000000)
	{
        Arch/i386/Boot/boot.ao
		*(.multiboot)
		*(.page_tables)
		*(.text)
		*(.text.startup)
	}

	.rodata ALIGN(4K) : AT(ADDR(.rodata) - 0xc0000000)
	{
		start_ctors = .;
		*(.ctors)
		end_ctors = .;

		*(.rodata)
	}

	.data ALIGN(4K) : AT(ADDR(.data) - 0xc0000000)
	{
		*(.data)
	}

	.bss ALIGN(4K) : AT(ADDR(.bss) - 0xc0000000)
	{
		*(COMMON)
		*(.bss)
	}
}
