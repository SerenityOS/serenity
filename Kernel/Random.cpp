/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/HPET.h>
#include <Kernel/Time/RTC.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

static Singleton<KernelRng> s_the;
static Atomic<u32, AK::MemoryOrder::memory_order_relaxed> s_next_random_value = 1;

KernelRng& KernelRng::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT KernelRng::KernelRng()
{
    bool supports_rdseed = Processor::current().has_feature(CPUFeature::RDSEED);
    bool supports_rdrand = Processor::current().has_feature(CPUFeature::RDRAND);
    if (supports_rdseed || supports_rdrand) {
        dmesgln("KernelRng: Using RDSEED or RDRAND as entropy source");
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
    } else if (TimeManagement::the().can_query_precise_time()) {
        // Add HPET as entropy source if we don't have anything better.
        dmesgln("KernelRng: Using HPET as entropy source");

        for (size_t i = 0; i < resource().pool_count * resource().reseed_threshold; ++i) {
            u64 hpet_time = HPET::the().read_main_counter_unsafe();
            this->resource().add_random_event(hpet_time, i % 32);
        }
    } else {
        // Fallback to RTC
        dmesgln("KernelRng: Using RTC as entropy source (bad!)");
        auto current_time = static_cast<u64>(RTC::now());
        for (size_t i = 0; i < resource().pool_count * resource().reseed_threshold; ++i) {
            this->resource().add_random_event(current_time, i % 32);
            current_time *= 0x574au;
            current_time += 0x40b2u;
        }
    }
}

void KernelRng::wait_for_entropy()
{
    SpinlockLocker lock(get_lock());
    if (!resource().is_ready()) {
        dbgln("Entropy starvation...");
        m_seed_queue.wait_forever("KernelRng");
    }
}

void KernelRng::wake_if_ready()
{
    VERIFY(get_lock().is_locked());
    if (resource().is_ready()) {
        m_seed_queue.wake_all();
    }
}

size_t EntropySource::next_source { static_cast<size_t>(EntropySource::Static::MaxHardcodedSourceIndex) };

static void do_get_fast_random_bytes(Bytes buffer)
{

    union {
        u8 bytes[4];
        u32 value;
    } u;
    size_t offset = 4;
    for (size_t i = 0; i < buffer.size(); ++i) {
        if (offset >= 4) {
            auto current_next = s_next_random_value.load();
            for (;;) {
                auto new_next = current_next * 1103515245 + 12345;
                if (s_next_random_value.compare_exchange_strong(current_next, new_next)) {
                    u.value = new_next;
                    break;
                }
            }
            offset = 0;
        }
        buffer[i] = u.bytes[offset++];
    }
}

bool get_good_random_bytes(Bytes buffer, bool allow_wait, bool fallback_to_fast)
{
    bool result = false;
    auto& kernel_rng = KernelRng::the();
    // FIXME: What if interrupts are disabled because we're in an interrupt?
    bool can_wait = are_interrupts_enabled();
    if (!can_wait && allow_wait) {
        // If we can't wait but the caller would be ok with it, then we
        // need to definitely fallback to *something*, even if it's less
        // secure...
        fallback_to_fast = true;
    }
    if (can_wait && allow_wait) {
        for (;;) {
            {
                MutexLocker locker(KernelRng::the().lock());
                if (kernel_rng.resource().get_random_bytes(buffer)) {
                    result = true;
                    break;
                }
            }
            kernel_rng.wait_for_entropy();
        }
    } else {
        // We can't wait/block here, or we are not allowed to block/wait
        if (kernel_rng.resource().get_random_bytes(buffer)) {
            result = true;
        } else if (fallback_to_fast) {
            // If interrupts are disabled
            do_get_fast_random_bytes(buffer);
            result = true;
        }
    }

    // NOTE: The only case where this function should ever return false and
    // not actually return random data is if fallback_to_fast == false and
    // allow_wait == false and interrupts are enabled!
    VERIFY(result || !fallback_to_fast);
    return result;
}

void get_fast_random_bytes(Bytes buffer)
{
    // Try to get good randomness, but don't block if we can't right now
    // and allow falling back to fast randomness
    auto result = get_good_random_bytes(buffer, false, true);
    VERIFY(result);
}

}
