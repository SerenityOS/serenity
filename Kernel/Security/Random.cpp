/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/Processor.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Time/HPET.h>
#    include <Kernel/Arch/x86_64/Time/RTC.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/ASM_wrapper.h>
#endif
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
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
#if ARCH(X86_64)
    if (Processor::current().has_feature(CPUFeature::RDSEED)) {
        dmesgln("KernelRng: Using RDSEED as entropy source");

        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            add_random_event(Kernel::read_rdseed(), i % 32);
        }
    } else if (Processor::current().has_feature(CPUFeature::RDRAND)) {
        dmesgln("KernelRng: Using RDRAND as entropy source");

        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            add_random_event(Kernel::read_rdrand(), i % 32);
        }
    } else if (TimeManagement::the().can_query_precise_time()) {
        // Add HPET as entropy source if we don't have anything better.
        dmesgln("KernelRng: Using HPET as entropy source");

        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            u64 hpet_time = HPET::the().read_main_counter_unsafe();
            add_random_event(hpet_time, i % 32);
        }
    } else {
        // Fallback to RTC
        dmesgln("KernelRng: Using RTC as entropy source (bad!)");
        auto current_time = static_cast<u64>(RTC::now());
        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            add_random_event(current_time, i % 32);
            current_time *= 0x574au;
            current_time += 0x40b2u;
        }
    }
#elif ARCH(AARCH64)
    if (Processor::current().has_feature(CPUFeature::RNG)) {
        dmesgln("KernelRng: Using RNDRRS as entropy source");
        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            add_random_event(Aarch64::Asm::read_rndrrs(), i % 32);
        }
    } else {
        // Fallback to TimeManagement as entropy
        dmesgln("KernelRng: Using bad entropy source TimeManagement");
        auto current_time = static_cast<u64>(TimeManagement::now().milliseconds_since_epoch());
        for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
            add_random_event(current_time, i % 32);
            current_time *= 0x574au;
            current_time += 0x40b2u;
        }
    }
#elif ARCH(RISCV64)
    // Fallback to TimeManagement as entropy
    dmesgln("KernelRng: Using bad entropy source TimeManagement");
    auto current_time = static_cast<u64>(TimeManagement::now().milliseconds_since_epoch());
    for (size_t i = 0; i < pool_count * reseed_threshold; ++i) {
        add_random_event(current_time, i % 32);
        current_time *= 0x574au;
        current_time += 0x40b2u;
    }
#else
    dmesgln("KernelRng: No entropy source available!");
#endif
}

void KernelRng::wait_for_entropy()
{
    SpinlockLocker lock(get_lock());
    if (!is_ready()) {
        dbgln("Entropy starvation...");
        m_seed_queue.wait_forever("KernelRng"sv);
    }
}

void KernelRng::wake_if_ready()
{
    VERIFY(get_lock().is_locked());
    if (is_ready()) {
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
    bool can_wait = Processor::are_interrupts_enabled();
    if (!can_wait && allow_wait) {
        // If we can't wait but the caller would be ok with it, then we
        // need to definitely fallback to *something*, even if it's less
        // secure...
        fallback_to_fast = true;
    }
    if (can_wait && allow_wait) {
        for (;;) {
            {
                if (kernel_rng.get_random_bytes(buffer)) {
                    result = true;
                    break;
                }
            }
            kernel_rng.wait_for_entropy();
        }
    } else {
        // We can't wait/block here, or we are not allowed to block/wait
        if (kernel_rng.get_random_bytes(buffer)) {
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
