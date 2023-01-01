.code32

/* multiboot1 */
.section .multiboot, "a"

.set MULTIBOOT_MAGIC,         0x1badb002
.set MULTIBOOT_PAGE_ALIGN,    0x1
.set MULTIBOOT_MEMORY_INFO,   0x2
.set MULTIBOOT_VIDEO_MODE,    0x4
.set multiboot_flags,         MULTIBOOT_PAGE_ALIGN | MULTIBOOT_MEMORY_INFO
.set multiboot_checksum,      -(MULTIBOOT_MAGIC + multiboot_flags)

.align 4
multiboot_header:
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
multiboot_header_end:

/* multiboot2 */
.set MULTIBOOT2_MAGIC,         0xe85250d6
.set MULTIBOOT2_ARCH,          0x0
.set MULTIBOOT2_LENGTH,        (multiboot2_header_end - multiboot2_header)
.set MULTIBOOT2_CHECKSUM,      -(MULTIBOOT2_MAGIC + MULTIBOOT2_ARCH + MULTIBOOT2_LENGTH)

.align 8
multiboot2_header:

.long MULTIBOOT2_MAGIC
.long MULTIBOOT2_ARCH
.long MULTIBOOT2_LENGTH
.long MULTIBOOT2_CHECKSUM

/* Framebuffer tag */
.align 8
multiboot2_fb:
.short 5
.short 0
.long multiboot2_fb_end - multiboot2_fb
.long 1280          /* width */
.long 1024          /* height */
.long 32            /* depth */
multiboot2_fb_end:

/* End tag */
.align 8
.long 0
.long 0
.long 8
multiboot2_header_end:
