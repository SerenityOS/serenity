#include <Kernel/Sections.h>

.section .text

/*
  The apic_ap_start function will be loaded to P0x00008000 where the APIC
  will boot the AP from in real mode. This code also contains space for
  special variables that *must* remain here. When initializing the APIC,
  the code here gets copied to P0x00008000, the variables in here get
  populated and then the boot of the APs will be triggered.
  Having the variables here allows us to access them from real mode. Also, the
  code here avoids the need for relocation entries.

  Basically, the variables between apic_ap_start and end_apic_ap_start
  *MUST* remain here and cannot be moved into a .bss or any other location.
*/
.global apic_ap_start
.type apic_ap_start, @function
.align 8
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

    /* check if we support NX and enable it if we do */
    movl $0x80000001, %eax
    cpuid
    testl $0x100000, %edx
    je (1f - apic_ap_start + 0x8000)
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
    lgdt (ap_cpu_gdt64ptr - apic_ap_start + 0x8000)

    /* Jump into our identity mapped area, stay in low memory for now.
       We need to fully enable 64 bit mode before we can adjust rsp and rip
       to values higher than 4GB */
    ljmpl $(ap_cpu_gdt64code - ap_cpu_gdt64), $(apic_ap_start64 - apic_ap_start + 0x8000)
.code64
apic_ap_start64:
    movq (ap_cpu_kernel_map_base - apic_ap_start)(%rbp), %rbp
    addq $0x8000, %rbp

    /* find our allocated stack based on the generated id */
    movq (ap_cpu_init_stacks - apic_ap_start)(%rbp, %rsi, 8), %rsp

    /* Now jump from our identity mapped area into high memory */
    movq $(1f - apic_ap_start), %rax
    addq %rbp, %rax
    jmp *%rax
1:
    mov $0, %ax
    mov %ax, %ss
    mov %ax, %ds
    mov %ax, %es
    mov %ax, %fs
    mov %ax, %gs

    /* flush the TLB */
    movq %cr3, %rax
    movq %rax, %cr3

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


    /* Save the cpu id into rdi (first argument), 0 representing the bsp */
    movq %rsi, %rdi
    incq %rdi

    /* Save the Processor pointer this CPU is going to use into rsi (second argument) */
    movq (ap_cpu_init_processor_info_array - apic_ap_start)(%rbp), %rax
    movq 0(%rax, %rsi, 8), %rsi

    /* Get the entry function */
    movq (ap_cpu_kernel_entry_function - apic_ap_start)(%rbp), %r10

    movq %rsp, %rbp
    cld

    /* We are in identity mapped P0x8000 and the BSP will unload this code
       once all APs are initialized, so call the entry function and return to our
       infinite loop if it ever were to return. */
    call *%r10

loop:
    hlt
    jmp loop

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
.align 8
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
.global ap_cpu_gdt64
ap_cpu_gdt64:
    .8byte 0x0
.global ap_cpu_gdt64code
ap_cpu_gdt64code:
    .4byte 0xffff
    .4byte 0xaf9a00
.global ap_cpu_gdt64data
ap_cpu_gdt64data:
    .4byte 0xffff
    .4byte 0xaf9200
.global ap_cpu_gdt64ptr
ap_cpu_gdt64ptr:
    .2byte ap_cpu_gdt64ptr - ap_cpu_gdt64 - 1
    .8byte (ap_cpu_gdt64 - apic_ap_start) + 0x8000
.align 8
.global ap_cpu_kernel_entry_function
ap_cpu_kernel_entry_function:
    .8byte 0x0 /* will be set at runtime */
.global ap_cpu_kernel_map_base
ap_cpu_kernel_map_base:
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
