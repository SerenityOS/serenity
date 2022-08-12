ENTRY(start)

PHDRS
{
  boot_text PT_LOAD ;
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
        KEEP(*(.multiboot))
    } :boot_text

    .text ALIGN(4K) : AT (ADDR(.text))
    {
        start_of_prekernel_text = .;
        *(.text*)
    } :text

    .data ALIGN(4K) : AT (ADDR(.data))
    {
        *(.data*)
    } :data

    .rodata ALIGN(4K) : AT (ADDR(.rodata))
    {
        *(.rodata*)
    } :data

    .bss ALIGN(4K) (NOLOAD) : AT (ADDR(.bss))
    {
        *(COMMON)
        *(.bss)
        *(.stack)
        *(.page_tables)
    } :bss

    end_of_prekernel_image = .;

    .Kernel_drow ALIGN(4K) : AT (ADDR(.Kernel_drow))
    {
        _binary_Kernel_drow_standalone_start = .;
        KEEP(*(.Kernel_drow))
    }

    end_of_prekernel_image_after_kernel_image = .;

}
