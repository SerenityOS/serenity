ENTRY(start)

PHDRS
{
  /* FIXME: Use defined constants like in other linker scripts */
  entry_stub_text PT_LOAD FLAGS(0x1 | 0x4);
  entry_stub_data PT_LOAD FLAGS(0x2 | 0x4);
  text PT_LOAD FLAGS(0x1 | 0x4);
  data PT_LOAD FLAGS(0x2 | 0x4);
  rodata PT_LOAD FLAGS(0x4);
  bss PT_LOAD FLAGS(0x2 | 0x4);
}

/* We need to tell the linker that the prekernel will execute at 0x7'0000. */ 
MEMORY
{
  PREKERNEL       (rwx) : ORIGIN = 0x70000, LENGTH = 0x010000
  ENTRY_STUB      (rwx) : ORIGIN = 0x80000, LENGTH = 0x001000
  RPI_LOAD_REGION (rwx) : ORIGIN = 0x81000, LENGTH = 0x100000
}

RPI_ENTRY = 0x80000;
PREKERNEL_BASE = 0x70000;

SECTIONS
{
    . = RPI_ENTRY;

    /*
       The entry stub is separated from the rest of the code,
       since everything else gets moved downwards straight away by the Prekernel.
    */
    .entry_stub.text ALIGN(4K) :
    {
        start_of_entry_stub = .;
        *(.entry_stub.text)
    } > ENTRY_STUB :entry_stub_text

    .entry_stub.data ALIGN(8) :
    {
        *(.entry_stub.data)
        end_of_entry_stub = .;
    }  > ENTRY_STUB :entry_stub_data

    . = PREKERNEL_BASE;

    .text ALIGN(4K) : ALIGN(4K)
    {
        start_of_prekernel_image = .;
        *(.text.first)
        *(.text*)
    } > PREKERNEL AT>RPI_LOAD_REGION :text

    .rodata ALIGN(4K) : ALIGN(4K)
    {
        start_ctors = .;
        *(.init_array)
        end_ctors = .;

        *(.rodata*)
    } > PREKERNEL AT>RPI_LOAD_REGION :rodata

    .data ALIGN(4K) : ALIGN(4K)
    {
        *(.data*)
    } > PREKERNEL AT>RPI_LOAD_REGION :data

    .bss ALIGN(4K) (NOLOAD) : ALIGN(4K)
    {
        start_of_bss = .;
        *(.bss)
        end_of_bss = .;
    } > PREKERNEL AT>RPI_LOAD_REGION :bss

    end_of_prekernel_image = .;
}

size_of_prekernel_image = end_of_prekernel_image - start_of_prekernel_image;
size_of_prekernel_image_divided_by_8 = (size_of_prekernel_image + 7) / 8;
prekernel_load_base = LOADADDR(.text);
size_of_bss_divided_by_8 = (end_of_bss - start_of_bss + 7) / 8;
load_init = init - ADDR(.text) + LOADADDR(.text);
