.set MULTIBOOT_MAGIC,         0x1badb002
.set MULTIBOOT_PAGE_ALIGN,    0x1
.set MULTIBOOT_MEMORY_INFO,   0x2
.set MULTIBOOT_VIDEO_MODE,    0x4
.set multiboot_flags,         MULTIBOOT_PAGE_ALIGN | MULTIBOOT_MEMORY_INFO | MULTIBOOT_VIDEO_MODE
.set multiboot_checksum,      -(MULTIBOOT_MAGIC + multiboot_flags)

.section .multiboot
.align 4

.long MULTIBOOT_MAGIC
.long multiboot_flags
.long multiboot_checksum


/* for MULTIBOOT_MEMORY_INFO */
.long 0x00000000    /* header_addr */
.long 0x00000000    /* load_addr */
.long 0x00000000    /* load_end_addr */
.long 0x00000000    /* bss_end_addr */
.long 0x00000000    /* entry_addr */

/* for MULTIBOOT_VIDEO_MODE */
.long 0x00000000    /* mode_type */
.long 1280          /* width */
.long 1024          /* height */
.long 32            /* depth */

.section .stack, "aw", @nobits
stack_bottom:
.skip 32768
stack_top:

.section .page_tables
.align 4096
page_tables_start:
.skip 4096*3

.section .text

.global start
.type start, @function

.extern init 
.type init, @function

.extern multiboot_info_ptr
.type multiboot_info_ptr, @object

start:
    cli
    cld

    mov $stack_top, %esp

    and $-16, %esp

    mov %ebx, multiboot_info_ptr

    pushl $page_tables_start
    call init
    add $4, %esp

    pushl $exit_message
    call kprintf
    add $4, %esp

    cli

loop:
    hlt
    jmp loop

exit_message:
    .asciz "Kernel exited."
