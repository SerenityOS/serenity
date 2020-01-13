
/**
 * Note that the initial inspiration (read: copy paste) for this code came from .rtld_start in NetBSD. However,
 * Their code does funky NetBSD specific stuff (like, their kernel passes some kind of special struct on the stack?)
 * that makes this not really the same assembly anymore.
 */

    .text
    .align    4
    .globl    _ld_elf_start
    .hidden   _ld_elf_start
    .type     _ld_elf_start,@function
_ld_elf_start:
	popl	%edx # pop 0 into edx
                 # stack contains argc, argv, envp
    call    _ld_elf_priv_pc_thunk
                 # EBX now contains the address of the following lea instruction
1:
    leal    _DYNAMIC-1b(%edx),%ecx    # &_DYNAMIC
                 # ECX now contains the real address of _DYNAMIC (.dynamic)
    movl   %ecx, %ebx
                 # EBX now contains a copy of _DYNAMIC
    subl    _GLOBAL_OFFSET_TABLE_-1b(%edx),%ebx
                 # EBX = real address of _DYNAMIC - [real address of GOT]
                 # Where first entry in GOT is offset of dynamic from image base
                 # So EBX = relocation base address, because the first entry in the
                 # GOT is the offset of _DYNAMIC from the beginning of the program image
    pushl    %ebx            # base_addr
    pushl    %ecx            # &_DYNAMIC
    call    ld_elf_relocate_self

    leal	8(%esp),%eax	 # orig ESP (i.e argc)

    pushl    %ebx            # base_addr
    pushl    %eax            # sp 
                             # We want the stack pointer so we can use
                             # argc, argv, and envp without popping them from the stack
    call    ld_elf_main      # ld_elf_main(sp, relocbase)

    addl	$12,%esp		# pop ... something. If we don't do this, then the args to main are wrong 
                            # Ask the NetBSD guys I guess...
    jmp    *%eax            # Finally, jump to the 'main' returned from ld_elf_main (probably main but you never know)

    .align 4
    .globl _ld_elf_priv_pc_thunk
    .hidden _ld_elf_priv_pc_thunk
    .type _ld_elf_priv_pc_thunk,@function
_ld_elf_priv_pc_thunk:
    mov    (%esp), %edx
    ret
    nop
