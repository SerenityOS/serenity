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
        *(.bss)
    } :bss
}
