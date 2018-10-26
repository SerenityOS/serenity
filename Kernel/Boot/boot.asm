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

    mov     bx, 0x1000
    mov     es, bx
    xor     bx, bx              ; Load kernel @ 0x10000

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
    cmp cx, 600
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
    dw 9
sectors_per_track:
    dw 18
heads:
    dw 2

msg_sectors_loaded:
    db "sectors loaded", 0x0d, 0x0a, 0

message:
    db "boot!", 0x0d, 0x0a, 0

fug_message:
    db "FUG!", 0x0d, 0x0a, 0

times 510-($-$$) db 0
dw 0xaa55
