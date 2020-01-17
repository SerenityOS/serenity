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

.section .page_tables, "aw", @nobits
.align 4096
.global boot_pdpt
boot_pdpt:
.skip 4096
.global boot_pd0
boot_pd0:
.skip 4096
.global boot_pd3
boot_pd3:
.skip 4096
.global boot_pd3_pde1023_pt
boot_pd3_pde1023_pt:
.skip 4096

.section .text

.global start
.type start, @function

.extern init 
.type init, @function

.extern multiboot_info_ptr
.type multiboot_info_ptr, @object

/*
    construct the following (32-bit PAE) page table layout:

pdpt

    0: boot_pd0 (0-1GB)
    1: n/a      (1-2GB)
    2: n/a      (2-3GB)
    3: boot_pd3 (3-4GB)

boot_pd0 : 512 pde's

    0: (0-2MB) (id 2MB page)
    1: (2-4MB) (id 2MB page)
    2: (4-6MB) (id 2MB page)
    3: (6-8MB) (id 2MB page)

boot_pd3 : 512 pde's

    0: boot_pd3_pde0 (3072-3074MB) (pseudo)
    1: boot_pd3_pde1 (3074-3076MB) (pseudo)
    2: boot_pd3_pde2 (3076-3078MB) (pseudo)
    3: boot_pd3_pde3 (3078-3080MB) (pseudo)
    4: boot_pd3_pde1023_pt (4094-4096MB) (for page table mappings)
*/

start:
    cli
    cld

    /* clear pdpt */
    movl $(boot_pdpt - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pdpt[0] and pdpt[3] */
    movl $(boot_pdpt - 0xc0000000), %edi
    movl $((boot_pd0 - 0xc0000000) + 1), 0(%edi)
    movl $((boot_pd3 - 0xc0000000) + 1), 24(%edi)

    /* clear pd0 */
    movl $(boot_pd0 - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* identity map bottom 8MB using 2MB pages (only PDE, no PTE) */
    movl $4, %ecx
    xorl %eax, %eax
    movl $(boot_pd0 - 0xc0000000), %edi
1:
    movl %eax, 0(%edi)
    /* PS(2MB) + R/W + Present */
    orl $0x83, 0(%edi)

    addl $8, %edi
    addl $(1048576 * 2), %eax
    loop 1b

    /* clear pd3 */
    movl $(boot_pd3 - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* pseudo-identity map first 8MB above 3GB mark using 2MB pages again */
    movl $4, %ecx
    xorl %eax, %eax
    movl $(boot_pd3 - 0xc0000000), %edi
1:
    movl %eax, 0(%edi)
    /* PS(2MB) + R/W + Present */
    orl $0x83, 0(%edi)

    addl $8, %edi
    addl $(1048576 * 2), %eax
    loop 1b

    /* create an empty page table for the top 2MB at the 4GB mark */
    movl $(boot_pd3 - 0xc0000000), %edi
    movl $(boot_pd3_pde1023_pt - 0xc0000000), 4088(%edi)
    orl $0x3, 4088(%edi)
    movl $0, 4092(%edi)

    /* point CR3 to PDPT */
    movl $(boot_pdpt - 0xc0000000), %eax
    movl %eax, %cr3

    /* enable PAE + PSE */
    movl %cr4, %eax
    orl $0x60, %eax
    movl %eax, %cr4

    /* enable PG */
    movl %cr0, %eax
    orl $0x80000000, %eax
    movl %eax, %cr0

    /* jmp to an address above the 3GB mark */
    push %cs
    push $1f
    retf
1:

    movl %cr3, %eax
    movl %eax, %cr3

    /* set up initial stack and jump into C++ land */
    mov $stack_top, %esp
    and $-16, %esp

    addl $0xc0000000, %ebx
    movl %ebx, multiboot_info_ptr

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
