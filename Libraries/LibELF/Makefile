OBJS = LdElf.o \
	   ELFImage.o \
	   ELFDynamicObject.o \
	   ELFDynamicLoader.o

EXTRA_OBJS = Arch/i386/ld_elf_entry.ao \
			 Arch/i386/plt_trampoline.ao

PROGRAM = ld-elf.so

SUBPROJECT_CXXFLAGS = -fvisibility=hidden -fpic -nostartfiles -ffreestanding -nostdlib -pie -fPIE -fno-stack-protector -mno-80387 -mno-mmx -mno-sse -mno-sse2 -fno-asynchronous-unwind-tables
LDFLAGS= -Wl,-e,_ld_elf_start -pie -shared

include ../../Makefile.common
