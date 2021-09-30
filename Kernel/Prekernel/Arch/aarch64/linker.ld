ENTRY(start)

PHDRS
{
  text PT_LOAD ;
  data PT_LOAD ;
  bss PT_LOAD ;
}

SECTIONS
{
    . = 0x00080000;

    .text ALIGN(4K) : AT (ADDR(.text))
    {
        *(.text.first)
        *(.text*)
    } :text

    .rodata ALIGN(4K) : AT (ADDR(.rodata))
    {
        *(.rodata*)
    } :data

    .data ALIGN(4K) : AT (ADDR(.data))
    {
        *(.data*)
    } :data

    .bss ALIGN(4K) (NOLOAD) : AT (ADDR(.bss))
    {
	start_of_bss = .;
        *(.bss)
	end_of_bss = .;
    } :bss
}

size_of_bss_divided_by_8 = (end_of_bss - start_of_bss) / 8;
