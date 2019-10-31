ENTRY(start)

SECTIONS
{
	. = 0x10000;

	.text BLOCK(4K) : ALIGN(4K)
	{
        Arch/i386/Boot/boot.ao
		*(.multiboot)
		*(.text)
		*(.text.startup)
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
		*(.data)
	}

	.bss BLOCK(4K) : ALIGN(4K)
	{
		*(COMMON)
		*(.bss)
	}
}
