; asmsyntax=nasm

[org 0x7c00]
[bits 16]

boot:
    push cs
    pop ds
    xor bx, bx
    mov ah, 0x0e
    mov si, message
    lodsb
.lewp:
    int 0x10
    lodsb
    cmp al, 0
    jne .lewp

    mov bx, 0x1000
    mov es, bx
    xor bx, bx              ; Load kernel @ 0x10000

    mov ah, 0x02            ; cmd 0x02 - Read Disk Sectors
    mov al, 72              ; 72 sectors (max allowed by bochs BIOS)
    mov ch, 0               ; track 0
    mov cl, 10              ; sector 10
    mov dh, 0               ; head 0
    mov dl, 0               ; drive 0 (fd0)
    int 0x13

    jc fug

    mov ah, 0x02
    mov al, 32
    add bx, 0x9000
    mov ch, 2
    mov cl, 10
    mov dh, 0
    mov dl, 0
    int 0x13

    jc fug

    lgdt [cs:test_gdt_ptr]

    mov eax, cr0
    or al, 1
    mov cr0, eax

    jmp 0x08:pmode

pmode:
[bits 32]
    mov ax, 0x10
    mov ds, ax
    mov es, ax
    mov fs, ax
    mov gs, ax

    mov ss, ax
    mov esp, 0x2000

    jmp 0x10000

    hlt

test_gdt_ptr:
    dw (test_gdt_end-test_gdt)
    dd test_gdt

test_gdt:
    dd 0
    dd 0
    dd 0x0000ffff
    dd 0x00cf9a00
    dd 0x0000ffff
    dd 0x00cf9200
    dd 0
    dd 0
    dd 0
    dd 0
test_gdt_end:

[bits 16]
fug:
    xor bx, bx
    mov ah, 0x0e
    mov si, fug_message
    lodsb
.lewp:
    int 0x10
    lodsb
    cmp al, 0
    jne .lewp

    cli
    hlt
    
message:
    db "boot!", 0x0d, 0x0a, 0

fug_message:
    db "FUG!", 0x0d, 0x0a, 0

times 510-($-$$) db 0
dw 0xaa55
