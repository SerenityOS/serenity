ENTRY(start)

PHDRS
{
  boot_text PT_LOAD ;
  boot_bss PT_LOAD ;
  text PT_LOAD ;
  data PT_LOAD ;
  bss PT_LOAD ;
}

SECTIONS
{
    . = 0x00100000;

    start_of_prekernel_image = .;

    .boot_text ALIGN(4K) : AT (ADDR(.boot_text))
    {
        KEEP(*(.boot_text))
        KEEP(*(.multiboot))
    } :boot_text

    .boot_bss ALIGN(4K) (NOLOAD) : AT (ADDR(.boot_bss))
    {
        KEEP(*(.page_tables))
        KEEP(*(.stack))
        *(.super_pages)
    } :boot_bss

    .text ALIGN(4K) : AT (ADDR(.text))
    {
        start_of_kernel_text = .;
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
        *(COMMON)
        *(.bss)
    } :bss

    end_of_prekernel_image = .;
}
