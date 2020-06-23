/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

static KernelRng* s_the;

KernelRng& KernelRng::the()
{
    if (!s_the) {
        s_the = new KernelRng;
    }
    return *s_the;
}

KernelRng::KernelRng()
{
    if (g_cpu_supports_rdrand) {
        for (size_t i = 0; i < KernelRng::pool_count * KernelRng::reseed_threshold; ++i) {
            u32 value = 0;
            asm volatile(
                "1:\n"
                "rdseed %0\n"
                "jnc 1b\n"
                : "=r"(value));

            this->add_random_event(value, i % 32);
        }
    }
}

void get_good_random_bytes(u8* buffer, size_t buffer_size)
{
    KernelRng::the().get_random_bytes(buffer, buffer_size);
}

void get_fast_random_bytes(u8* buffer, size_t buffer_size)
{
    return get_good_random_bytes(buffer, buffer_size);
}

}
