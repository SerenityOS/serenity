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
.global boot_pd0_pt0
boot_pd0_pt0:
.skip 4096 * 4
.global boot_pd3_pts
boot_pd3_pts:
.skip 4096 * 4
.global boot_pd3_pt1023
boot_pd3_pt1023:
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

    0: boot_pd0_pt0 (0-2MB) (id 512 4KB pages)

boot_pd3 : 512 pde's

    0: boot_pd3_pts[0] (3072-3074MB) (pseudo 512 4KB pages)
    1: boot_pd3_pts[1] (3074-3076MB) (pseudo 512 4KB pages)
    2: boot_pd3_pts[2] (3076-3078MB) (pseudo 512 4KB pages)
    3: boot_pd3_pts[3] (3078-3080MB) (pseudo 512 4KB pages)
    4: boot_pd3_pt1023 (4094-4096MB) (for page table mappings)

the 9 page tables each contain 512 pte's that map individual 4KB pages

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
    movl $(1024 * 5), %ecx
    xorl %eax, %eax
    rep stosl

    /* add boot_pd0_pt0 to boot_pd0 */
    movl $(boot_pd0 - 0xc0000000), %edi
    movl $(boot_pd0_pt0 - 0xc0000000), %eax
    movl %eax, 0(%edi)
    /* R/W + Present */
    orl $0x3, 0(%edi)

    /* add boot_pd3_pts to boot_pd3 */
    movl $4, %ecx
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

    /* pseudo identity map the 3072-3080MB range */
    movl $(512 * 4), %ecx
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

    /* unmap the 0-1MB range, which isn't used after jmp-ing up here */
    movl $256, %ecx
    movl $(boot_pd0_pt0 - 0xc0000000), %edi
    xorl %eax, %eax

1:
    movl %eax, 0(%edi)
    addl $8, %edi
    loop 1b

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
    addl $0xc0000000, %eax
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

.align 4
.global apic_ap_start_size
apic_ap_start_size:
    .2byte end_apic_ap_start - apic_ap_start
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
