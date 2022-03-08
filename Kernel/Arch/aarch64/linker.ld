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

    /*
        FIXME: 8MB is enough space for all of the tables required to identity map 
               physical memory. 8M is wasteful, so this should be properly calculated.
    */
    
    . = ALIGN(4K);
    page_tables_phys_start = .;
    
    . += 8M;
    page_tables_phys_end = .;
}

size_of_bss_divided_by_8 = (end_of_bss - start_of_bss) / 8;
