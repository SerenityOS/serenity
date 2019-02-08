; asmsyntax=nasm

global setjmp
setjmp:
    mov eax, [esp + 4]
    mov [eax + 0 * 4], ebx
    mov [eax + 1 * 4], esi
    mov [eax + 2 * 4], edi
    mov [eax + 3 * 4], ebp
    lea ecx, [esp + 4]
    mov [eax + 4 * 4], ecx
    mov ecx, [esp]
    mov [eax + 5 * 4], ecx
    xor eax, eax
    ret

global longjmp
longjmp:
    mov edx, [esp + 4]
    mov eax, [esp + 8]
    mov ebx, [edx + 0 * 4]
    mov esi, [edx + 1 * 4]
    mov edi, [edx + 2 * 4]
    mov ebp, [edx + 3 * 4]
    mov ecx, [edx + 4 * 4]
    mov esp, ecx
    mov ecx, [edx + 5 * 4]
    test eax, eax
    jnz  .nonzero
    mov eax, 1
.nonzero:
    jmp ecx

