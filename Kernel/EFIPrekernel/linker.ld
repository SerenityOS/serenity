ENTRY(init)

SECTIONS
{
    /* This is symbol is used to tell the PrekernelPEImageGenerator that the PE base address is 0 */
    pe_image_base = .;

    /* PE doesn't have segments, so PE sections behave similarly to ELF segments. */

    /* PE sections have to be aligned by the section alignment specified in the windows-specific fields. We define it to be 4K.
       So all output sections that won't be discard by PrekernelPEImageGenerator have to be properly aligned. */

    /* Skip over the PE headers. The virtual addresses of all PE sections have to bigger than the size of the PE headers. */
    . = 0x1000;

    /* The output sections are named after https://learn.microsoft.com/en-us/windows/win32/debug/pe-format#special-sections.
       PrekernelPEImageGenerator will add the PE .reloc section by converting ELF R_*_RELATIVE relocations to PE base relocations. */

    .text ALIGN(4K) :
    {
        *(.text*)
    }

    .rdata ALIGN(4K) :
    {
        *(.rodata* .srodata*)
    }

    .data ALIGN(4K) :
    {
        *(.data* .sdata*)
    }

    .got ALIGN(4K) :
    {
        *(.got.plt)
        *(.got)
    }

    .bss ALIGN(4K) (NOLOAD) :
    {
        *(COMMON)
        *(.bss* .sbss*)
    }
}
