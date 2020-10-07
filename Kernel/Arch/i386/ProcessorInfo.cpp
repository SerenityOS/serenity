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

#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/ProcessorInfo.h>

namespace Kernel {

ProcessorInfo::ProcessorInfo(Processor& processor)
    : m_processor(processor)
{
    u32 max_leaf;
    {
        CPUID cpuid(0);
        StringBuilder builder;
        auto emit_u32 = [&](u32 value) {
            builder.appendf("%c%c%c%c",
                value & 0xff,
                (value >> 8) & 0xff,
                (value >> 16) & 0xff,
                (value >> 24) & 0xff);
        };
        max_leaf = cpuid.eax();
        emit_u32(cpuid.ebx());
        emit_u32(cpuid.edx());
        emit_u32(cpuid.ecx());
        m_cpuid = builder.build();
    }
    {
        ASSERT(max_leaf >= 1);
        CPUID cpuid(1);
        m_stepping = cpuid.eax() & 0xf;
        u32 model = (cpuid.eax() >> 4) & 0xf;
        u32 family = (cpuid.eax() >> 8) & 0xf;
        m_type = (cpuid.eax() >> 12) & 0x3;
        u32 extended_model = (cpuid.eax() >> 16) & 0xf;
        u32 extended_family = (cpuid.eax() >> 20) & 0xff;
        if (family == 15) {
            m_display_family = family + extended_family;
            m_display_model = model + (extended_model << 4);
        } else if (family == 6) {
            m_display_family = family;
            m_display_model = model + (extended_model << 4);
        } else {
            m_display_family = family;
            m_display_model = model;
        }
    }

    u32 max_extended_leaf = CPUID(0x80000000).eax();

    if (max_extended_leaf >= 0x80000004) {
        alignas(u32) char buffer[48];
        u32* bufptr = reinterpret_cast<u32*>(buffer);
        auto copy_brand_string_part_to_buffer = [&](u32 i) {
            CPUID cpuid(0x80000002 + i);
            *bufptr++ = cpuid.eax();
            *bufptr++ = cpuid.ebx();
            *bufptr++ = cpuid.ecx();
            *bufptr++ = cpuid.edx();
        };
        copy_brand_string_part_to_buffer(0);
        copy_brand_string_part_to_buffer(1);
        copy_brand_string_part_to_buffer(2);
        m_brandstr = buffer;
    }

    // Cache the CPU feature string
    m_features = m_processor.features_string();
}

}
