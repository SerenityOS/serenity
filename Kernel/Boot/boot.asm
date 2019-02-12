; asmsyntax=nasm

[org 0x7c00]
[bits 16]

boot:
    cli
    mov     ax, 0x8000
    mov     ss, ax
    mov     sp, 0xffff

    push    cs
    pop     ds
    xor     bx, bx
    mov     ah, 0x0e
    mov     si, message
    lodsb
.lewp:
    int     0x10
    lodsb
    cmp     al, 0
    jne     .lewp

    ; Enable A20
    mov     ax, 0x2401
    int     0x15

    ; HACK: Load the ELF kernel at 0xf000. Assuming that the first
    ;       LOAD header has a file offset of 0x1000, this puts _start
    ;       at 0x10000 which we jump to later.
    ;       This is all quite rickety.
    mov     bx, 0xf00
    mov     es, bx
    xor     bx, bx

    mov cx, word [cur_lba]
.sector_loop:
    call convert_lba_to_chs

    mov ah, 0x02            ; cmd 0x02 - Read Disk Sectors
    mov al, 1               ; 1 sector at a time
    mov dl, 0               ; drive 0 (fd0)
    int 0x13

    jc fug

    mov ah, 0x0e
    mov al, '.'
    int 0x10

    inc word [cur_lba]
    mov cx, word [cur_lba]
    cmp cx, 900
    jz .sector_loop_end

    mov bx, es
    add bx, 0x20
    mov es, bx
    xor bx, bx

    jmp .sector_loop

.sector_loop_end:

    call durk

    ; Turn off the floppy motor.
    mov dx, 0x3f2
    xor al, al
    out dx, al

    ; Let's look at the ELF header.
    mov bx, 0xf00
    mov fs, bx
    cmp [fs:0], dword 0x464c457f ; ELF magic: { 0x7f "ELF" }
    jne fug

    cmp [fs:24], dword 0x10000 ; Entry should be 0x10000
    jne fug

    mov ebx, dword [fs:28] ; EBX <- program header table
    mov ecx, dword [fs:44] ; ECX <- program header count

; Let's find the BSS and clear it.

parse_program_header:
    cmp [fs:ebx], dword 0x1 ; Is Load segment?
    jne .next

    cmp [fs:ebx+24], dword 0x6 ; Is read+write but not execute?
    jne .next

    mov edi, [fs:ebx+8] ; EDI <- p_vaddr
    add edi, [fs:ebx+16] ; skip over 'p_filesz' bytes (leave them intact)

    push ecx

    sub edi, [fs:ebx+16] ; skip over 'p_filesz' bytes (see above)

    ; Since we're in 16-bit real mode, create a segment address.
    mov eax, edi
    shr eax, 4
    mov es, ax
    and edi, 0xf

    mov ecx, [fs:ebx+20] ; ECX <- p_memsz
    xor al, al
    rep stosb

    pop ecx

.next:
    add ebx, 32
    loop parse_program_header

; Okay we're all set to go!

lets_go:
    lgdt [cs:test_gdt_ptr]

    mov eax, cr0
    or al, 1
    mov cr0, eax

    jmp 0x08:pmode

durk:
    push cs
    pop ds
    xor bx, bx
    mov ah, 0x0e
    mov si, msg_sectors_loaded
    lodsb
.lewp:
    int 0x10
    lodsb
    cmp al, 0
    jne .lewp
    ret

pmode:
[bits 32]
    mov ax, 0x10
    mov ds, ax
    mov es, ax
    mov fs, ax
    mov gs, ax

    mov ss, ax
    mov esp, 0x4000

    xor eax, eax
    xor ebx, ebx
    xor ecx, ecx
    xor edx, edx
    xor ebp, ebp
    xor esi, esi
    xor edi, edi

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

; Input:
;
; AX = LBA
;
; Output:
;
; CX and DH = C/H/S address formatted for Int13,2

; CL = sector (LBA % sectors_per_track) + 1
;
; 1.44M floppy stats:
; (sectors_per_track: 18)
; (heads: 2)
; (sectors: 2880)

convert_lba_to_chs:
    mov     ax, cx

    ; AX = LBA/spt, DX = LBA%spt
    xor     dx, dx
    div     word [sectors_per_track]

    ; CL = sector (LBA % sectors_per_track) + 1
    mov     cl, dl
    inc     cl

    ; CH = track (LBA / sectors_per_track) / heads
    mov     ch, al
    shr     ch, 1

    ; AX = (LBA/spt)/heads, DX = (LBA/spt)%heads
    xor     dx, dx
    div     word [heads]

    ; DH = sector (LBA / sectors_per_track) % heads
    mov     dh, dl

    ret 
    
cur_lba:
    dw 1
sectors_per_track:
    dw 18
heads:
    dw 2

msg_sectors_loaded:
    db "done!", 0x0d, 0x0a, 0

message:
    db "Loading kernel", 0

fug_message:
    db "FUG!", 0x0d, 0x0a, 0

times 510-($-$$) db 0
dw 0xaa55
