#include <AK/Platform.h>
#include <Kernel/Prekernel/Prekernel.h>

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
.skip (4096 * 4)
.global boot_pd0_pts
boot_pd0_pts:
.skip 4096 * (MAX_KERNEL_SIZE >> 21)
.global boot_pd_kernel
boot_pd_kernel:
.skip 4096
.global boot_pd_kernel_pt0
boot_pd_kernel_pt0:
.skip 4096
.global boot_pd_kernel_image_pts
boot_pd_kernel_image_pts:
.skip 4096 * (MAX_KERNEL_SIZE >> 21)
.global boot_pd_kernel_pt1023
boot_pd_kernel_pt1023:
.skip 4096

.section .text

.global start
.type start, @function

.extern init
.type init, @function

.global reload_cr3
.type reload_cr3, @function

.extern multiboot_info_ptr
.type multiboot_info_ptr, @object

/*
    construct the following (64-bit PML4T) page table layout:
    (the PML4T part is not used for 32-bit x86)

pml4t:

    0: pdpt (0-512GB)

pdpt

    0: boot_pd0             (0-1GB)
    1: boot_pd0 + 4096      (1-2GB)
    2: boot_pd0 + 4096 * 2  (2-3GB)
    3: boot_pd0 + 4096 * 3  (3-4GB)

boot_pd0 : 512 PDEs

    boot_pd0_pts (0MB - MAX_KERNEL_SIZE) (id 512 4KB pages)

the page tables each contain 512 PTEs that map individual 4KB pages

*/

gdt64:
    .quad 0
gdt64code:
    .quad (1<<43) | (1<<44) | (1<<47) | (1<<53) /* executable, code segment, present, 64-bit */
.global gdt64ptr
gdt64ptr:
    .short . - gdt64 - 1
    .quad gdt64

.set code64_sel_value, gdt64code - gdt64

.global code64_sel
code64_sel:
.short code64_sel_value

start:
    jmp real_start

/*
    param 1: pointer to C string
    returns: Length of string (including null byte)
*/
print_no_halt:
    pushl %ebp
    movl %esp, %ebp

    pushl %esi
    pushl %ecx

    movl 8(%ebp), %esi

    mov $0xb8000, %ecx    /* VRAM address. */
    mov $0x07, %ah        /* grey-on-black text. */

.Lprint_str_loop:
    lodsb                 /* Loads a byte from address at %esi into %al and increments %esi. */

    test %al, %al
    jz .Lprint_str_end

    movw %ax, (%ecx)
    add $2, %ecx

    jmp .Lprint_str_loop
.Lprint_str_end:

    mov %esi, %eax
    sub 8(%ebp), %eax

    popl %ecx
    popl %esi

    movl %ebp, %esp
    popl %ebp
    ret



/*
    this function assumes that paging is disabled (or everything is mapped 1:1)
    param 1: pointer to string ended with null terminator (C string)
*/
print_and_halt:

/*  from now on, we don't really care about booting because we are missing required CPU features such as PAE or long mode.
    the flow from now is like so:
    1. Copy all necessary parts to low memory section in RAM
    2. Jump to that section
    3. In that section we do:
        a. exit protected mode to pure 16 bit real mode
        b. load the "<missing feature> is not supported" String, call the BIOS print to screen service
        c. halt
*/

.equ COPIED_STRING_LOCATION, 0x400
.equ GDT_REAL_MODE_LOCATION, 0x45000
.equ EXITING_PROTECTED_MODE_CODE_LOCATION, 0x10000
.equ REAL_MODE_CODE, 0x500
.equ PROTECTED_MODE_16_BIT_CODE, 0x600
    movl %esp, %ebp
    movl 4(%ebp), %esi

    /* Print string using non-destructive methods */
    pushl %esi
    call print_no_halt
    addl $4, %esp

    /* print_no_halt returns the string length (including null byte) in eax. */
    mov %eax, %ecx
    movw %cx, (COPIED_STRING_LOCATION)    /* Store string length for later use. */

    /* Copy string into lower memory */
    mov 4(%ebp), %esi
    mov $COPIED_STRING_LOCATION + 2, %edi
    rep movsb

    /* Copy gdt_table_real_mode to low memory section  */
    movl $gdt_table_real_mode, %eax
    movl $gdt_table_real_mode_end, %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $GDT_REAL_MODE_LOCATION, %edi     /* destination address of the code */
    rep movsb

    /* Copy protected_mode_16_bit to real_mode to low memory section  */
    movl $protected_mode_16_bit, %eax
    movl $real_mode, %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $PROTECTED_MODE_16_BIT_CODE, %edi     /* destination address of the code */
    rep movsb

    /* Copy real_mode to end_of_print_and_halt_function to low memory section  */
    movl $real_mode, %eax
    movl $end_of_print_and_halt_function, %ebx

    movl %ebx, %ecx
    sub %eax, %ecx
    mov %eax, %esi          /* source address of the code */
    mov $REAL_MODE_CODE, %edi     /* destination address of the code */
    rep movsb


    /* Copy all opcodes from exiting_real_mode label to protected_mode_16_bit label to low memory RAM  */
    movl $exiting_real_mode, %eax
    movl $protected_mode_16_bit, %ebx

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

no_pae_string:
    .asciz "Your computer does not support PAE. Halting!"

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
    lret
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
    lret
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
    mov $end_of_prekernel_image, %esi
    cmp $MAX_KERNEL_SIZE, %esi
    jbe kernel_not_too_large

    movl $kernel_image_too_big_string, %esi
    pushl %esi
    call print_and_halt
    /* We should not return, but just in case, halt */
    hlt

kernel_not_too_large:
    /* test for PAE presence, save the most important registers from corruption */
    pushl %eax
    pushl %edx
    pushl %ebx

    movl $0x1, %eax       /* PAE presence is in CPUID input 0x1 */
    cpuid
    testl $(1 << 6), %edx /* Test if the PAE-bit, which is bit 6, is set in the edx register. */
    jnz pae_supported     /* If the bit is not set, there is no PAE capability. */

    /* Since there is no PAE capability, halt with an error message */
    movl $no_pae_string, %esi
    pushl %esi
    call print_and_halt
    /* We should not return, but just in case, halt */
    hlt

pae_supported:
    movl $0x80000001, %eax
    cpuid
    testl $(1 << 29), %edx   /* Test if the LM-bit, which is bit 29, is set in the edx register. */
    jnz long_mode_supported             /* If LM-bit is not enabled, there is no long mode. */

    /* Since there is no long mode, halt with an error message */
    movl $no_long_mode_string, %esi
    pushl %esi
    call print_and_halt
    /* We should not return, but just in case, halt */
    hlt


/* If both PAE and long mode is supported, continue with booting the system */

long_mode_supported:
    /* restore the pushed registers and continue with booting */
    popl %ebx
    popl %edx
    popl %eax

    /* We don't know where the bootloader might have put the command line.
     * It might be at an inconvenient location that we're not about to map,
     * so let's just copy it to a convenient location while we have the whole
     * memory space identity-mapped anyway. :^)
     */

    movl %ebx, %esi
    addl $16, %esi
    movl (%esi), %esi
    movl $1024, %ecx
    movl $kernel_cmdline, %edi
    rep movsl

    /* clear pml4t */
    movl $boot_pml4t, %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pml4t[0] */
    movl $boot_pml4t, %edi
    movl $boot_pdpt, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* clear pdpt */
    movl $boot_pdpt, %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pdpt[0]..pdpt[3] */
    movl $boot_pdpt, %edi
    movl $(boot_pd0 + 3), 0(%edi)
    movl $(boot_pd0 + 4096 + 3), 8(%edi)
    movl $(boot_pd0 + 4096 * 2 + 3), 16(%edi)
    movl $(boot_pd0 + 4096 * 3 + 3), 24(%edi)

    /* clear pd0 */
    movl $boot_pd0, %edi
    movl $4096, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd0's PTs */
    movl $boot_pd0_pts, %edi
    movl $(1024 * (MAX_KERNEL_SIZE >> 21)), %ecx
    xorl %eax, %eax
    rep stosl

    /* add boot_pd0_pts to boot_pd0 */
    movl $(MAX_KERNEL_SIZE >> 21), %ecx
    movl $boot_pd0, %edi
    movl $boot_pd0_pts, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* identity map the 0MB to MAX_KERNEL_SIZE range */
    movl $(512 * (MAX_KERNEL_SIZE >> 21)), %ecx
    movl $boot_pd0_pts, %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* Map the rest with 2MiB pages */
    /* add boot_pd0_pts to boot_pd0 */
    movl $(2048 - (MAX_KERNEL_SIZE >> 21)), %ecx
    movl $(boot_pd0 + (MAX_KERNEL_SIZE >> 21) * 8), %edi
    /* R/W + Present + 2 MiB */
    movl $(MAX_KERNEL_SIZE | 0x83), %eax

1:
    movl %eax, 0(%edi)
    addl $8, %edi
    addl $(1 << 21), %eax
    loop 1b

    /* point CR3 to PML4T */
    movl $boot_pml4t, %eax

    movl %eax, %cr3

    /* enable PAE + PSE */
    movl %cr4, %eax
    orl $0x60, %eax
    movl %eax, %cr4

1:
    /* Enter Long-mode! ref(https://wiki.osdev.org/Setting_Up_Long_Mode)*/
    mov $0xC0000080, %ecx           /* Set the C-register to 0xC0000080, which is the EFER MSR.*/
    rdmsr                           /* Read from the model-specific register.*/
    or $(1 << 8), %eax              /* Set the LM-bit which is the 9th bit (bit 8).*/
    wrmsr                           /* Write to the model-specific register.*/

    /* enable PG */
    movl %cr0, %eax
    orl $0x80000000, %eax
    movl %eax, %cr0

    /* set up stack */
    mov $stack_top, %esp
    and $-16, %esp

    /* Now we are in 32-bit compatibility mode, We still need to load a 64-bit GDT */
    mov $gdt64ptr, %eax
    lgdt (%eax)
    ljmpl $code64_sel_value, $1f

.code64
1:
    movl %ebx, %ebx
    movq %rbx, multiboot_info_ptr

    mov $0, %ax
    mov %ax, %ss
    mov %ax, %ds
    mov %ax, %es
    mov %ax, %fs
    mov %ax, %gs

    call reload_cr3
    call init

    cli
loop:
    hlt
    jmp loop

reload_cr3:
    pushq %rax
    mov %cr3, %rax
    mov %rax, %cr3
    popq %rax
    ret
