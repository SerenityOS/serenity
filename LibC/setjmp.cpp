#include <setjmp.h>
#include <assert.h>
#include <Kernel/Syscall.h>

asm(
".globl setjmp\n\
setjmp:\n\
    movl %ebx, 0(%eax)\n\
    movl %esi, 4(%eax)\n\
    movl %edi, 8(%eax)\n\
    movl %ebp, 12(%eax)\n\
    movl %esp, 16(%eax)\n\
    movl (%esp), %ecx\n\
    movl %ecx, 20(%eax)\n\
    xorl %eax, %eax\n\
    ret\n\
");

asm(
".globl longjmp\n\
longjmp:\n\
    xchgl %edx, %eax\n\
    test %eax, %eax\n\
    jnz 1f\n\
    incl %eax\n\
1:\n\
    mov 0(%edx), %ebx\n\
    mov 4(%edx), %esi\n\
    mov 8(%edx), %edi\n\
    mov 12(%edx), %ebp\n\
    mov 16(%edx), %ecx\n\
    mov %ecx, %esp\n\
    mov 20(%edx), %ecx\n\
    jmp *%ecx\n\
");
