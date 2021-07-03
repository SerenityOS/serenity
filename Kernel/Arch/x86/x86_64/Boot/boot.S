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
.global boot_pml4t
boot_pml4t:
.skip 4096
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

gdt64:
    .quad 0
gdt64code:
    .quad (1<<43) | (1<<44) | (1<<47) | (1<<53) /* executable, code segment, present, 64-bit */
gdt64ptr:
    .short . - gdt64 - 1
    .quad gdt64

.set code64_sel,  gdt64code - gdt64

start:
    cli
    cld

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

    /* We don't know where the bootloader might have put the command line.
     * It might be at an inconvenient location that we're not about to map,
     * so let's just copy it to a convenient location while we have the whole
     * memory space identity-mapped anyway. :^)
     */

    movl %ebx, %esi
    addl $16, %esi
    movl (%esi), %esi
    movl $1024, %ecx
    movl $(kernel_cmdline - 0xc0000000), %edi
    rep movsl

    /* clear pml4t */
    movl $(boot_pml4t - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pml4t[0] */
    movl $(boot_pml4t - 0xc0000000), %edi
    movl $(boot_pdpt - 0xc0000000), 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* clear pdpt */
    movl $(boot_pdpt - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* set up pdpt[0] and pdpt[3] */
    movl $(boot_pdpt - 0xc0000000), %edi
    movl $((boot_pd0 - 0xc0000000) + 3), 0(%edi)
    movl $((boot_pd3 - 0xc0000000) + 3), 24(%edi)

    /* clear pd0 */
    movl $(boot_pd0 - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3 */
    movl $(boot_pd3 - 0xc0000000), %edi
    movl $1024, %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd0's pt's */
    movl $(boot_pd0_pt0 - 0xc0000000), %edi
    movl $(1024 * 4), %ecx
    xorl %eax, %eax
    rep stosl

    /* clear pd3's pt's */
    movl $(boot_pd3_pts - 0xc0000000), %edi
    movl $(1024 * 17), %ecx
    xorl %eax, %eax
    rep stosl

    /* add boot_pd0_pt0 to boot_pd0 */
    movl $(boot_pd0 - 0xc0000000), %edi
    movl $(boot_pd0_pt0 - 0xc0000000), %eax
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* add boot_pd3_pts to boot_pd3 */
    movl $16, %ecx
    movl $(boot_pd3 - 0xc0000000), %edi
    movl $(boot_pd3_pts - 0xc0000000), %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* identity map the 0 to 2MB range */
    movl $512, %ecx
    movl $(boot_pd0_pt0 - 0xc0000000), %edi
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
    movl $(boot_pd3_pts - 0xc0000000), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)
    addl $8, %edi
    addl $4096, %eax
    loop 1b

    /* create an empty page table for the top 2MB at the 4GB mark */
    movl $(boot_pd3 - 0xc0000000), %edi
    movl $(boot_pd3_pt1023 - 0xc0000000), 4088(%edi)
    orl $0x3, 4088(%edi)
    movl $0, 4092(%edi)

    /* point CR3 to PML4T */
    movl $(boot_pml4t - 0xc0000000), %eax
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
    /* Now we are in 32-bit compatibility mode, We still need to load a 64-bit GDT */

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
    movl $(boot_pd0_pt0 - 0xc0000000), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    addl $8, %edi
    loop 1b

    /* jump into C++ land */
    addl $0xc0000000, %ebx
    movl %ebx, multiboot_info_ptr

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

    call init
    add $4, %rsp

    cli
loop:
    hlt
    jmp loop

.extern init_ap
.type init_ap, @function

/*
  The apic_ap_start function will be loaded to P0x00008000 where the APIC
  will boot the AP from in real mode. This code also contains space for
  special variables that *must* remain here. When initializing the APIC,
  the code here gets copied to P0x00008000, the variables in here get
  populated and then the the boot of the APs will be triggered. Having
  the variables here allows us to access them from real mode. Also, the
  code here avoids the need for relocation entries.

  Basically, the variables between apic_ap_start and end_apic_ap_start
  *MUST* remain here and cannot be moved into a .bss or any other location.
*/
.global apic_ap_start
.type apic_ap_start, @function
apic_ap_start:
.code16
    cli
    jmp $0x800, $(1f - apic_ap_start) /* avoid relocation entries */
1:
    mov %cs, %ax
    mov %ax, %ds

    xor %ax, %ax
    mov %ax, %sp

    /* load the first temporary gdt */
    lgdt (ap_cpu_gdtr_initial - apic_ap_start)

    /* enable PM */
    movl %cr0, %eax
    orl $1, %eax
    movl %eax, %cr0

    ljmpl $8, $(apic_ap_start32 - apic_ap_start + 0x8000)
apic_ap_start32:
.code32
    mov $0x10, %ax
    mov %ax, %ss
    mov %ax, %ds
    mov %ax, %es
    mov %ax, %fs
    mov %ax, %gs

    movl $0x8000, %ebp

    /* generate a unique ap cpu id (0 means 1st ap, not bsp!) */
    xorl %eax, %eax
    incl %eax
    lock; xaddl %eax, (ap_cpu_id - apic_ap_start)(%ebp) /* avoid relocation entries */
    movl %eax, %esi

    /* find our allocated stack based on the generated id */
    movl (ap_cpu_init_stacks - apic_ap_start)(%ebp, %eax, 4), %esp

    /* check if we support NX and enable it if we do */
    movl $0x80000001, %eax
    cpuid
    testl $0x100000, %edx
    // TODO: Uncomment this
    //je (1f - apic_ap_start + 0x8000)
    /* turn on IA32_EFER.NXE */
    movl $0xc0000080, %ecx
    rdmsr
    orl $0x800, %eax
    wrmsr
1:

    /* load the bsp's cr3 value */
    movl (ap_cpu_init_cr3 - apic_ap_start)(%ebp), %eax
    movl %eax, %cr3

    /* Enter Long-mode! ref(https://wiki.osdev.org/Setting_Up_Long_Mode)*/
    mov $0xC0000080, %ecx           /* Set the C-register to 0xC0000080, which is the EFER MSR.*/
    rdmsr                           /* Read from the model-specific register.*/
    or $(1 << 8), %eax              /* Set the LM-bit which is the 9th bit (bit 8).*/
    wrmsr                           /* Write to the model-specific register.*/

    /* enable PAE + PSE */
    movl %cr4, %eax
    orl $0x60, %eax
    movl %eax, %cr4

    /* enable PG */
    movl %cr0, %eax
    orl $0x80000000, %eax
    movl %eax, %cr0

    /* load the temporary 64-bit gdt from boot that points above 3GB */
    lgdt gdt64ptr

    /* jump above 3GB into our identity mapped area now */
    ljmpl $code64_sel, $(apic_ap_start64 - apic_ap_start + 0xc0008000)
.code64
apic_ap_start64:
    mov $0, %ax
    mov %ax, %ss
    mov %ax, %ds
    mov %ax, %es
    mov %ax, %fs
    mov %ax, %gs

    /* flush the TLB */
    movq %cr3, %rax
    movq %rax, %cr3

    movl $0xc0008000, %ebp

    /* now load the final gdt and idt from the identity mapped area */
    movq (ap_cpu_gdtr - apic_ap_start)(%rbp), %rax
    lgdt (%rax)
    movq (ap_cpu_idtr - apic_ap_start)(%rbp), %rax
    lidt (%rax)

    /* set same cr0 and cr4 values as the BSP */
    movq (ap_cpu_init_cr0 - apic_ap_start)(%rbp), %rax
    movq %rax, %cr0
    movq (ap_cpu_init_cr4 - apic_ap_start)(%rbp), %rax
    movq %rax, %cr4

    /* push the Processor pointer this CPU is going to use */
    movq (ap_cpu_init_processor_info_array - apic_ap_start)(%ebp), %rax
    movq $0xc0000000, %r8
    addq %r8, %rax
    movq 0(%rax, %rsi, 4), %rax
    push %rax

    /* push the cpu id, 0 representing the bsp and call into c++ */
    incq %rsi
    push %rsi

    xor %ebp, %ebp
    cld

    /* We are in identity mapped P0x8000 and the BSP will unload this code
       once all APs are initialized, so call init_ap but return to our
       infinite loop */
    movabs $loop, %rax
    pushq %rax
    movabs $init_ap, %rax
    jmp *(%rax)

.align 4
.global apic_ap_start_size
apic_ap_start_size:
    .2byte end_apic_ap_start - apic_ap_start
.align 4
ap_cpu_id:
    .4byte 0x0
ap_cpu_gdt:
    /* null */
    .8byte 0x0
    /* code */
    .4byte 0x0000FFFF
    .4byte 0x00cf9a00
    /* data */
    .4byte 0x0000FFFF
    .4byte 0x00cf9200
ap_cpu_gdt_end:
ap_cpu_gdtr_initial:
    .2byte ap_cpu_gdt_end - ap_cpu_gdt - 1
    .4byte (ap_cpu_gdt - apic_ap_start) + 0x8000
.global ap_cpu_gdtr
ap_cpu_gdtr:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_idtr
ap_cpu_idtr:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr0
ap_cpu_init_cr0:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr3
ap_cpu_init_cr3:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr4
ap_cpu_init_cr4:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_init_processor_info_array
ap_cpu_init_processor_info_array:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_init_stacks
ap_cpu_init_stacks:
    /* array of allocated stack pointers */
    /* NOTE: ap_cpu_init_stacks must be the last variable before
             end_apic_ap_start! */
.set end_apic_ap_start, .
