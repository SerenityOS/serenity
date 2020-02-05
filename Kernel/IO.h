/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/VMWareBackdoor.h>

namespace IO {

inline u8 in8(u16 port)
{
    u8 value;
    asm volatile("inb %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline u16 in16(u16 port)
{
    u16 value;
    asm volatile("inw %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline u32 in32(u16 port)
{
    u32 value;
    asm volatile("inl %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline void repeated_in16(u16 port, u8* buffer, int buffer_size)
{
    asm volatile("rep insw"
                 : "+D"(buffer), "+c"(buffer_size)
                 : "d"(port)
                 : "memory");
}

inline void out8(u16 port, u8 value)
{
    asm volatile("outb %0, %1" ::"a"(value), "Nd"(port));
}

inline void out16(u16 port, u16 value)
{
    asm volatile("outw %0, %1" ::"a"(value), "Nd"(port));
}

inline void out32(u16 port, u32 value)
{
    asm volatile("outl %0, %1" ::"a"(value), "Nd"(port));
}

inline void repeated_out16(u16 port, const u8* data, int data_size)
{
    asm volatile("rep outsw"
                 : "+S"(data), "+c"(data_size)
                 : "d"(port));
}

inline void vmware_out(VMWareCommand& command)
{
    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT;
    command.si = 0;
    command.di = 0;
    asm volatile("in %%dx, %0"
                 : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

inline void vmware_highbandwidth_send(VMWareCommand& command) {

    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT_HIGHBANDWIDTH;

	asm volatile("cld; rep; outsb" : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

inline void  vmware_highbandwidth_get(VMWareCommand& command) {
    command.magic = VMWARE_MAGIC;
    command.port = VMWARE_PORT_HIGHBANDWIDTH;
	asm volatile("cld; rep; insb" : "+a"(command.ax), "+b"(command.bx), "+c"(command.cx), "+d"(command.dx), "+S"(command.si), "+D"(command.di));
}

inline void delay()
{
    // ~3 microsecs
    for (auto i = 0; i < 32; i++) {
        IO::in8(0x80);
    }
}

}
