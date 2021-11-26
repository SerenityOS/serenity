#include <Kernel/Sections.h>

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

    /* enable PAE + PSE */
    movl %cr4, %eax
    orl $0x60, %eax
    movl %eax, %cr4

    /* enable PG */
    movl %cr0, %eax
    orl $0x80000000, %eax
    movl %eax, %cr0

    /* load a second temporary gdt that points above 3GB */
    lgdt (ap_cpu_gdtr_initial2 - apic_ap_start + 0xc0008000)

    /* jump above 3GB into our identity mapped area now */
    ljmp $8, $(apic_ap_start32_2 - apic_ap_start + 0xc0008000)
apic_ap_start32_2:
    /* flush the TLB */
    movl %cr3, %eax
    movl %eax, %cr3

    movl $0xc0008000, %ebp

    /* now load the final gdt and idt from the identity mapped area */
    movl (ap_cpu_gdtr - apic_ap_start)(%ebp), %eax
    lgdt (%eax)
    movl (ap_cpu_idtr - apic_ap_start)(%ebp), %eax
    lidt (%eax)

    /* set same cr0 and cr4 values as the BSP */
    movl (ap_cpu_init_cr0 - apic_ap_start)(%ebp), %eax
    movl %eax, %cr0
    movl (ap_cpu_init_cr4 - apic_ap_start)(%ebp), %eax
    movl %eax, %cr4

    /* push the Processor pointer this CPU is going to use */
    movl (ap_cpu_init_processor_info_array - apic_ap_start)(%ebp), %eax
    addl kernel_mapping_base, %eax
    movl 0(%eax, %esi, 4), %eax
    push %eax

    /* push the cpu id, 0 representing the bsp and call into c++ */
    incl %esi
    push %esi

    xor %ebp, %ebp
    cld

    /* We are in identity mapped P0x8000 and the BSP will unload this code
       once all APs are initialized, so call init_ap but return to our
       infinite loop */
    push $loop
    ljmp $8, $init_ap

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
ap_cpu_gdtr_initial2:
    .2byte ap_cpu_gdt_end - ap_cpu_gdt - 1
    .4byte (ap_cpu_gdt - apic_ap_start) + 0xc0008000
.global ap_cpu_gdtr
ap_cpu_gdtr:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_idtr
ap_cpu_idtr:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr0
ap_cpu_init_cr0:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr3
ap_cpu_init_cr3:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_init_cr4
ap_cpu_init_cr4:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_init_processor_info_array
ap_cpu_init_processor_info_array:
    .4byte 0x0 /* will be set at runtime */
.global ap_cpu_init_stacks
ap_cpu_init_stacks:
    /* array of allocated stack pointers */
    /* NOTE: ap_cpu_init_stacks must be the last variable before
             end_apic_ap_start! */
.set end_apic_ap_start, .
