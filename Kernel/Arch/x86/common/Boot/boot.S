#define _BOOTLOADER
#include <AK/Platform.h>

.code32
.set KERNEL_VIRTUAL_BASE, 0xc0000000

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
    cli
    cld

#if ARCH(X86_64)
    /* test for long mode presence, save the most important registers from corruption */
    pushl %eax
    pushl %edx
    pushl %ebx

    movl $0x80000001, %eax
    cpuid
    testl $(1 << 29), %edx   /* Test if the LM-bit, which is bit 29, is set in the edx register. */
    jnz continue             /* If LM-bit is not enabled, there is no long mode. */
    hlt

continue:
    /* restore the pushed registers */
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
    movl $(kernel_cmdline - KERNEL_VIRTUAL_BASE), %edi
    rep movsl

#if ARCH(X86_64)
    /* clear pml4t */
    movl $(boot_pml4t - KERNEL_VIRTUAL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pml4t[0] */
    movl $(boot_pml4t - KERNEL_VIRTUAL_BASE), %edi
    movl $(boot_pdpt - KERNEL_VIRTUAL_BASE), 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
#endif

    /* clear pdpt */
    movl $(boot_pdpt - KERNEL_VIRTUAL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pdpt[0] and pdpt[3] */
    movl $(boot_pdpt - KERNEL_VIRTUAL_BASE), %edi
#if ARCH(X86_64)
    movl $((boot_pd0 - KERNEL_VIRTUAL_BASE) + 3), 0(%edi)
    movl $((boot_pd3 - KERNEL_VIRTUAL_BASE) + 3), 24(%edi)
#else
    movl $((boot_pd0 - KERNEL_VIRTUAL_BASE) + 1), 0(%edi)
    movl $((boot_pd3 - KERNEL_VIRTUAL_BASE) + 1), 24(%edi)
#endif

    /* clear pd0 */
    movl $(boot_pd0 - KERNEL_VIRTUAL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3 */
    movl $(boot_pd3 - KERNEL_VIRTUAL_BASE), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd0's pt's */
    movl $(boot_pd0_pt0 - KERNEL_VIRTUAL_BASE), %edi
    movl $(1024 * 4), %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3's pt's */
    movl $(boot_pd3_pts - KERNEL_VIRTUAL_BASE), %edi
    movl $(1024 * 17), %ecx
    xorl %eax, %eax
    rep stosl

    /* add boot_pd0_pt0 to boot_pd0 */
    movl $(boot_pd0 - KERNEL_VIRTUAL_BASE), %edi
    movl $(boot_pd0_pt0 - KERNEL_VIRTUAL_BASE), %eax
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* add boot_pd3_pts to boot_pd3 */
    movl $16, %ecx
    movl $(boot_pd3 - KERNEL_VIRTUAL_BASE), %edi
    movl $(boot_pd3_pts - KERNEL_VIRTUAL_BASE), %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* identity map the 0 to 2MB range */
    movl $512, %ecx
    movl $(boot_pd0_pt0 - KERNEL_VIRTUAL_BASE), %edi
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
    movl $(boot_pd3_pts - KERNEL_VIRTUAL_BASE), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* create an empty page table for the top 2MB at the 4GB mark */
    movl $(boot_pd3 - KERNEL_VIRTUAL_BASE), %edi
    movl $(boot_pd3_pt1023 - KERNEL_VIRTUAL_BASE), 4088(%edi)
    orl $0x3, 4088(%edi)
    movl $0, 4092(%edi)

#if ARCH(X86_64)
    /* point CR3 to PML4T */
    movl $(boot_pml4t - KERNEL_VIRTUAL_BASE), %eax
#else
    /* point CR3 to PDPT */
    movl $(boot_pdpt - KERNEL_VIRTUAL_BASE), %eax
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
    movl $(boot_pd0_pt0 - KERNEL_VIRTUAL_BASE), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    addl $8, %edi
    loop 1b

    /* jump into C++ land */
    addl $KERNEL_VIRTUAL_BASE, %ebx
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

