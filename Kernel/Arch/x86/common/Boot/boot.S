#define _BOOTLOADER
#include <AK/Platform.h>
#include <Kernel/Sections.h>

.code32

.section .stack, "aw", @nobits
stack_bottom:
.skip 32768
stack_top:

.global kernel_cmdline
kernel_cmdline:
.skip 4096

.section .page_tables, "aw", @nobits
.align 4096
#if ARCH(X86_64)
.global boot_pml4t
boot_pml4t:
.skip 4096
#endif
.global boot_pdpt
boot_pdpt:
.skip 4096
.global boot_pd0
boot_pd0:
.skip 4096
.global boot_pd3
boot_pd3:
.skip 4096
.global boot_pd0_pt0
boot_pd0_pt0:
.skip 4096 * 4
.global boot_pd3_pts
boot_pd3_pts:
.skip 4096 * 16
.global boot_pd3_pt1023
boot_pd3_pt1023:
.skip 4096

.section .boot_text, "ax"

.global start
.type start, @function

.extern init
.type init, @function

.extern multiboot_info_ptr
.type multiboot_info_ptr, @object

/*
    construct the following (64-bit PML4T) page table layout:
    (the PML4T part is not used for 32-bit x86)

pml4t:

    0: pdpt (0-512GB)

pdpt

    0: boot_pd0 (0-1GB)
    1: n/a      (1-2GB)
    2: n/a      (2-3GB)
    3: boot_pd3 (3-4GB)

boot_pd0 : 512 pde's

    0: boot_pd0_pt0 (0-2MB) (id 512 4KB pages)

boot_pd3 : 512 pde's

    0: boot_pd3_pts[0] (3072-3074MB) (pseudo 512 4KB pages)
    1: boot_pd3_pts[1] (3074-3076MB) (pseudo 512 4KB pages)
    2: boot_pd3_pts[2] (3076-3078MB) (pseudo 512 4KB pages)
    3: boot_pd3_pts[3] (3078-3080MB) (pseudo 512 4KB pages)
    4: boot_pd3_pts[4] (3080-3082MB) (pseudo 512 4KB pages)
    5: boot_pd3_pts[5] (3082-3084MB) (pseudo 512 4KB pages)
    6: boot_pd3_pts[6] (3084-3086MB) (pseudo 512 4KB pages)
    7: boot_pd3_pts[7] (3086-3088MB) (pseudo 512 4KB pages)
    
    8: boot_pd3_pts[8] (3088-3090MB) (pseudo 512 4KB pages)
    9: boot_pd3_pts[9] (3090-3076MB) (pseudo 512 4KB pages)
    10: boot_pd3_pts[10] (3092-3094MB) (pseudo 512 4KB pages)
    11: boot_pd3_pts[11] (3094-3096MB) (pseudo 512 4KB pages)
    12: boot_pd3_pts[12] (3096-3098MB) (pseudo 512 4KB pages)
    13: boot_pd3_pts[13] (3098-3100MB) (pseudo 512 4KB pages)
    14: boot_pd3_pts[14] (3100-3102MB) (pseudo 512 4KB pages)
    15: boot_pd3_pts[15] (3102-3104MB) (pseudo 512 4KB pages)

    16: boot_pd3_pt1023 (4094-4096MB) (for page table mappings)

the 9 page tables each contain 512 pte's that map individual 4KB pages

*/

#if ARCH(X86_64)
gdt64:
    .quad 0
gdt64code:
    .quad (1<<43) | (1<<44) | (1<<47) | (1<<53) /* executable, code segment, present, 64-bit */
.global gdt64ptr
gdt64ptr:
    .short . - gdt64 - 1
    .quad gdt64

.global code64_sel
.set code64_sel,  gdt64code - gdt64
#endif

start:
    jmp real_start

/* 
    this function assumes that paging is disabled (or everything is mapped 1:1)
    param 1: pointer to string ended with null terminator (C string)
*/
print_and_halt:

.equ COPIED_STRING_LOCATION, 0x400
.equ GDT_REAL_MODE_LOCATION, 0x45000
.equ EXITING_PROTECTED_MODE_CODE_LOCATION, 0x10000
.equ REAL_MODE_CODE, 0x500
.equ PROTECTED_MODE_16_BIT_CODE, 0x600
    mov %esp, %ebp
    mov 4(%ebp), %edi

    /* Copy string to low memory section  */
    mov %edi, %esi
    xor %ecx, %ecx

    pushl %eax
    pushl %edi
check_string_length:
    movb (%edi), %ah
    cmp $0, %ah
    je check_string_length_exit
    inc %ecx
    inc %edi
    jmp check_string_length
check_string_length_exit:
    popl %edi
    popl %eax

    /* source address of the code is ESI */
    movw %cx, (COPIED_STRING_LOCATION)
    mov $COPIED_STRING_LOCATION + 2, %edi     /* destination address of the code */
    rep movsb

    /* Copy gdt_table_real_mode to low memory section  */
    movl $(gdt_table_real_mode - KERNEL_BASE), %eax
    movl $(gdt_table_real_mode_end - KERNEL_BASE), %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $GDT_REAL_MODE_LOCATION, %edi     /* destination address of the code */
    rep movsb

    /* Copy protected_mode_16_bit to real_mode to low memory section  */
    movl $(protected_mode_16_bit - KERNEL_BASE), %eax
    movl $(real_mode - KERNEL_BASE), %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $PROTECTED_MODE_16_BIT_CODE, %edi     /* destination address of the code */
    rep movsb

    /* Copy real_mode to end_of_print_and_halt_function to low memory section  */
    movl $(real_mode - KERNEL_BASE), %eax
    movl $(end_of_print_and_halt_function - KERNEL_BASE), %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $REAL_MODE_CODE, %edi     /* destination address of the code */
    rep movsb


    /* Copy all opcodes from exiting_real_mode label to protected_mode_16_bit label to low memory RAM  */
    movl $(exiting_real_mode - KERNEL_BASE), %eax
    movl $(protected_mode_16_bit - KERNEL_BASE), %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $EXITING_PROTECTED_MODE_CODE_LOCATION, %edi     /* destination address of the code */
    pushl %edi
    rep movsb
    popl %edi
    pushl %edi
    ret

gdt_table_real_mode:
    .quad 0             /* Empty entry */

	.short 0xffff
	.short 0
    .byte 0
    .byte 0b10011010
    .byte 0b00001111
    .byte 0x0

    .short 0xffff
	.short 0
    .byte 0
    .byte 0b10010010
    .byte 0b00001111
    .byte 0x0
gdt_table_real_mode_end:

no_long_mode_string:
    .asciz "Your computer does not support long mode (64-bit mode). Halting!"

kernel_image_too_big_string:
    .asciz "Error: Kernel Image too big for memory slot. Halting!"

/*
    This part is completely standalone - it doesn't involve any location from this
    near code. It uses arbitrary locations in the low memory section of the RAM.
    We don't really worry about where are these locations, because we only want to quickly
    print a string and halt.
*/
.code32
exiting_real_mode:

    /* Build IDT pointer and load it */
    mov $0x50000, %eax
    pushl %eax
    movl $0x3ff, 0(%eax)
    add $2, %eax
    movl $0, 0(%eax)
    popl %eax
    lidt (%eax)

    /* Build GDT pointer and load it */
    mov $0x40000, %eax
    pushl %eax
    movl $32, 0(%eax)
    add $2, %eax
    movl $GDT_REAL_MODE_LOCATION, 0(%eax)
    popl %eax
    lgdt (%eax)

    /* far jump to protected_mode_16_bit in 0x5000 */
    pushw $8
    push $PROTECTED_MODE_16_BIT_CODE
    retf
    hlt

.code16
protected_mode_16_bit:
    xor %eax, %eax
    movl $0x10, %eax
    movw %ax, %ds
    and $0xFE, %al       /* switch to pure real mode */
    mov %eax, %cr0
    mov $0x10, %eax
    movl %eax, %cr0

    pushw $0
    push $REAL_MODE_CODE
    retf
    hlt

real_mode:
    movw $0x7000, %ax
    movl $0x0000, %esp
    movw %ax, %ss

    xor %ax, %ax
    movw %ax, %ds
    movw %ax, %es
    movw %ax, %fs
    movw %ax, %gs

    mov $0x3, %ax
    int $0x10

    movb $0x13, %ah
    movb $0x0, %bh
    movb $0xf, %bl
    movw (COPIED_STRING_LOCATION), %cx
    movw $0, %dx
    movw $COPIED_STRING_LOCATION + 2, %bp
    int $0x10

    movl $0xdeadcafe, %ebx
    cli
    hlt
end_of_print_and_halt_function:

.code32
real_start:
    cli
    cld
    mov $end_of_kernel_image, %esi
    cmp $0xc2000000, %esi
    jbe kernel_not_too_large
    
    movl $(kernel_image_too_big_string - KERNEL_BASE), %esi
    pushl %esi
    call print_and_halt
    /* We should not return, but just in case, halt */
    hlt

kernel_not_too_large:


#if ARCH(X86_64)
    /* test for long mode presence, save the most important registers from corruption */
    pushl %eax
    pushl %edx
    pushl %ebx

    movl $0x80000001, %eax
    cpuid
    testl $(1 << 29), %edx   /* Test if the LM-bit, which is bit 29, is set in the edx register. */
    jnz long_mode_supported             /* If LM-bit is not enabled, there is no long mode. */

    /*  from now on, we don't really care about booting because we don't have long mode supported.
         the flow from now is like so:
         1. Copy all necessary parts to low memory section in RAM
         2. Jump to that section
         3. In that section we do:
            a. exit protected mode to pure 16 bit real mode
            b. load the "Long mode is not supported" String, call the BIOS print to screen service
            c. halt
    */
    movl $(no_long_mode_string - KERNEL_BASE), %esi
    pushl %esi
    call print_and_halt
    /* We should not return, but just in case, halt */
    hlt


/* If long mode is supported, continue with booting the system */

.code32
long_mode_supported:
    /* restore the pushed registers and continue with booting */
    popl %ebx
    popl %edx
    popl %eax
#endif

    /* We don't know where the bootloader might have put the command line.
     * It might be at an inconvenient location that we're not about to map,
     * so let's just copy it to a convenient location while we have the whole
     * memory space identity-mapped anyway. :^)
     */

    movl %ebx, %esi
    addl $16, %esi
    movl (%esi), %esi
    movl $1024, %ecx
    movl $(kernel_cmdline - KERNEL_BASE), %edi
    rep movsl

#if ARCH(X86_64)
    /* clear pml4t */
    movl $(boot_pml4t - KERNEL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pml4t[0] */
    movl $(boot_pml4t - KERNEL_BASE), %edi
    movl $(boot_pdpt - KERNEL_BASE), 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
#endif

    /* clear pdpt */
    movl $(boot_pdpt - KERNEL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pdpt[0] and pdpt[3] */
    movl $(boot_pdpt - KERNEL_BASE), %edi
#if ARCH(X86_64)
    movl $((boot_pd0 - KERNEL_BASE) + 3), 0(%edi)
    movl $((boot_pd3 - KERNEL_BASE) + 3), 24(%edi)
#else
    movl $((boot_pd0 - KERNEL_BASE) + 1), 0(%edi)
    movl $((boot_pd3 - KERNEL_BASE) + 1), 24(%edi)
#endif

    /* clear pd0 */
    movl $(boot_pd0 - KERNEL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3 */
    movl $(boot_pd3 - KERNEL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd0's pt's */
    movl $(boot_pd0_pt0 - KERNEL_BASE), %edi
    movl $(1024 * 4), %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3's pt's */
    movl $(boot_pd3_pts - KERNEL_BASE), %edi
    movl $(1024 * 17), %ecx
    xorl %eax, %eax
    rep stosl

    /* add boot_pd0_pt0 to boot_pd0 */
    movl $(boot_pd0 - KERNEL_BASE), %edi
    movl $(boot_pd0_pt0 - KERNEL_BASE), %eax
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* add boot_pd3_pts to boot_pd3 */
    movl $16, %ecx
    movl $(boot_pd3 - KERNEL_BASE), %edi
    movl $(boot_pd3_pts - KERNEL_BASE), %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* identity map the 0 to 2MB range */
    movl $512, %ecx
    movl $(boot_pd0_pt0 - KERNEL_BASE), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* pseudo identity map the 3072-3102MB range */
    movl $(512 * 16), %ecx
    movl $(boot_pd3_pts - KERNEL_BASE), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* create an empty page table for the top 2MB at the 4GB mark */
    movl $(boot_pd3 - KERNEL_BASE), %edi
    movl $(boot_pd3_pt1023 - KERNEL_BASE), 4088(%edi)
    orl $0x3, 4088(%edi)
    movl $0, 4092(%edi)

#if ARCH(X86_64)
    /* point CR3 to PML4T */
    movl $(boot_pml4t - KERNEL_BASE), %eax
#else
    /* point CR3 to PDPT */
    movl $(boot_pdpt - KERNEL_BASE), %eax
#endif

    movl %eax, %cr3

    /* enable PAE + PSE */
    movl %cr4, %eax
    orl $0x60, %eax
    movl %eax, %cr4

#if ARCH(X86_64)
1:
    /* Enter Long-mode! ref(https://wiki.osdev.org/Setting_Up_Long_Mode)*/
    mov $0xC0000080, %ecx           /* Set the C-register to 0xC0000080, which is the EFER MSR.*/
    rdmsr                           /* Read from the model-specific register.*/
    or $(1 << 8), %eax              /* Set the LM-bit which is the 9th bit (bit 8).*/
    wrmsr                           /* Write to the model-specific register.*/
#endif

    /* enable PG */
    movl %cr0, %eax
    orl $0x80000000, %eax
    movl %eax, %cr0

    /* set up stack */
    mov $stack_top, %esp
    and $-16, %esp

    /* jmp to an address above the 3GB mark */
    movl $1f,%eax
    jmp *%eax
1:
    movl %cr3, %eax
    movl %eax, %cr3

    /* unmap the 0-1MB range, which isn't used after jmp-ing up here */
    movl $256, %ecx
    movl $(boot_pd0_pt0 - KERNEL_BASE), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    addl $8, %edi
    loop 1b

    /* jump into C++ land */
    addl $KERNEL_BASE, %ebx
    movl %ebx, multiboot_info_ptr

#if ARCH(X86_64)
    /* Now we are in 32-bit compatibility mode, We still need to load a 64-bit GDT */
    lgdt gdt64ptr
    ljmpl $code64_sel, $1f

.code64
1:
    mov $0, %ax
    mov %ax, %ss
    mov %ax, %ds
    mov %ax, %es
    mov %ax, %fs
    mov %ax, %gs
#endif

    call init
#if ARCH(X86_64)
    add $4, %rsp
#else
    add $4, %esp
#endif

    cli
loop:
    hlt
    jmp loop

