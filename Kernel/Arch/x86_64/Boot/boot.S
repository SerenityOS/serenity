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

.global kernel_cmdline
kernel_cmdline:
.skip 4096

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
.skip 4096 * 8
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

start:
    cli
    cld

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

    call init
    add $4, %esp

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
    cli
    hlt

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
