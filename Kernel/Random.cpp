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

#include <AK/Singleton.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

static AK::Singleton<KernelRng> s_the;

KernelRng& KernelRng::the()
{
    return *s_the;
}

KernelRng::KernelRng()
{
    bool supports_rdseed = Processor::current().has_feature(CPUFeature::RDSEED);
    bool supports_rdrand = Processor::current().has_feature(CPUFeature::RDRAND);
    if (supports_rdseed || supports_rdrand) {
        for (size_t i = 0; i < resource().pool_count * resource().reseed_threshold; ++i) {
            u32 value = 0;
            if (supports_rdseed) {
                asm volatile(
                    "1:\n"
                    "rdseed %0\n"
                    "jnc 1b\n"
                    : "=r"(value));
            } else {
                asm volatile(
                    "1:\n"
                    "rdrand %0\n"
                    "jnc 1b\n"
                    : "=r"(value));
            }

            this->resource().add_random_event(value, i % 32);
        }
    }
}

void KernelRng::wait_for_entropy()
{
    if (!resource().is_ready()) {
        Thread::current()->wait_on(m_seed_queue, "KernelRng");
    }
}

void KernelRng::wake_if_ready()
{
    if (resource().is_ready()) {
        m_seed_queue.wake_all();
    }
}

size_t EntropySource::next_source { 0 };

void get_good_random_bytes(u8* buffer, size_t buffer_size)
{
    KernelRng::the().wait_for_entropy();

    // FIXME: What if interrupts are disabled because we're in an interrupt?
    if (are_interrupts_enabled()) {
        LOCKER(KernelRng::the().lock());
        KernelRng::the().resource().get_random_bytes(buffer, buffer_size);
    } else {
        KernelRng::the().resource().get_random_bytes(buffer, buffer_size);
    }
}

void get_fast_random_bytes(u8* buffer, size_t buffer_size)
{
    if (KernelRng::the().resource().is_ready()) {
        return get_good_random_bytes(buffer, buffer_size);
    }

    static u32 next = 1;

    union {
        u8 bytes[4];
        u32 value;
    } u;
    size_t offset = 4;
    for (size_t i = 0; i < buffer_size; ++i) {
        if (offset >= 4) {
            next = next * 1103515245 + 12345;
            u.value = next;
            offset = 0;
        }
        buffer[i] = u.bytes[offset++];
    }
}

}
