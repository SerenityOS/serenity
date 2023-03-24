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
        *(.stack)
        *(.page_tables)
    } :bss

    end_of_prekernel_image = .;

    .Kernel_image ALIGN(4K) : AT (ADDR(.Kernel_image))
    {
        _binary_Kernel_standalone_start = .;
        KEEP(*(.Kernel_image))
    }

    end_of_prekernel_image_after_kernel_image = .;

}
