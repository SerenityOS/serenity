/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Locking/Mutex.h>
#include <LibCrypto/Cipher/AES.h>
#include <LibCrypto/Cipher/Cipher.h>
#include <LibCrypto/Hash/SHA2.h>

namespace Kernel {

template<typename CipherT, typename HashT, int KeySize>
class FortunaPRNG {
public:
    constexpr static size_t pool_count = 32;
    constexpr static size_t reseed_threshold = 16;

    using CipherType = CipherT;
    using BlockType = typename CipherT::BlockType;
    using HashType = HashT;
    using DigestType = typename HashT::DigestType;

    // FIXME: Do something other than VERIFY()'ing in case of OOM.
    FortunaPRNG()
        : m_counter(ByteBuffer::create_zeroed(BlockType::block_size()).release_value_but_fixme_should_propagate_errors())
    {
    }

    bool get_random_bytes(Bytes buffer)
    {
        SpinlockLocker lock(m_lock);
        if (!is_ready())
            return false;
        if (m_p0_len >= reseed_threshold) {
            this->reseed();
        }

        VERIFY(is_seeded());

        // FIXME: More than 2^20 bytes cannot be generated without refreshing the key.
        VERIFY(buffer.size() < (1 << 20));

        typename CipherType::CTRMode cipher(m_key, KeySize, Crypto::Cipher::Intent::Encryption);

        auto counter_span = m_counter.bytes();
        cipher.key_stream(buffer, counter_span, &counter_span);

        // Extract a new key from the prng stream.
        Bytes key_span = m_key.bytes();
        cipher.key_stream(key_span, counter_span, &counter_span);
        return true;
    }

    template<typename T>
    void add_random_event(T const& event_data, size_t pool)
    {
        pool %= pool_count;
        if (pool == 0) {
            m_p0_len++;
        }
        m_pools[pool].update(reinterpret_cast<u8 const*>(&event_data), sizeof(T));
    }

    [[nodiscard]] bool is_seeded() const
    {
        return m_reseed_number > 0;
    }

    [[nodiscard]] bool is_ready() const
    {
        VERIFY(m_lock.is_locked());
        return is_seeded() || m_p0_len >= reseed_threshold;
    }

    Spinlock<LockRank::None>& get_lock() { return m_lock; }

private:
    void reseed()
    {
        HashType new_key;
        new_key.update(m_key);
        for (size_t i = 0; i < pool_count; ++i) {
            if (m_reseed_number % (1u << i) == 0) {
                DigestType digest = m_pools[i].digest();
                new_key.update(digest.immutable_data(), digest.data_length());
            }
        }
        DigestType digest = new_key.digest();
        if (m_key.size() == digest.data_length()) {
            // Avoid reallocating, just overwrite the key.
            m_key.overwrite(0, digest.immutable_data(), digest.data_length());
        } else {
            auto buffer_result = ByteBuffer::copy(digest.immutable_data(), digest.data_length());
            // If there's no memory left to copy this into, bail out.
            if (buffer_result.is_error())
                return;

            m_key = buffer_result.release_value();
        }

        m_reseed_number++;
        m_p0_len = 0;
    }

    ByteBuffer m_counter;
    size_t m_reseed_number { 0 };
    size_t m_p0_len { 0 };
    ByteBuffer m_key;
    HashType m_pools[pool_count];
    Spinlock<LockRank::None> m_lock {};
};

class KernelRng : public FortunaPRNG<Crypto::Cipher::AESCipher, Crypto::Hash::SHA256, 256> {

public:
    KernelRng();
    static KernelRng& the();

    void wait_for_entropy();

    void wake_if_ready();

private:
    WaitQueue m_seed_queue;
};

class EntropySource {
    template<typename T>
    struct Event {
        u64 timestamp;
        size_t source;
        T event_data;
    };

public:
    enum class Static : size_t {
        Interrupts,
        MaxHardcodedSourceIndex,
    };

    EntropySource()
        : m_source(next_source++)
    {
    }

    EntropySource(Static hardcoded_source)
        : m_source(static_cast<size_t>(hardcoded_source))
    {
    }

    template<typename T>
    void add_random_event(T const& event_data)
    {
        auto& kernel_rng = KernelRng::the();
        SpinlockLocker lock(kernel_rng.get_lock());

        u64 timestamp = 0;
        auto maybe_cycle_count = Processor::read_cycle_count();
        if (maybe_cycle_count.has_value())
            timestamp = maybe_cycle_count.release_value();
        else
            timestamp = static_cast<u64>(TimeManagement::now().milliseconds_since_epoch());

        // We don't lock this because on the off chance a pool is corrupted, entropy isn't lost.
        Event<T> event = { timestamp, m_source, event_data };
        kernel_rng.add_random_event(event, m_pool);
        m_pool++;
        kernel_rng.wake_if_ready();
    }

private:
    static size_t next_source;
    size_t m_pool { 0 };
    size_t m_source;
};

// NOTE: These API's are primarily about expressing intent/needs in the calling code.
//       The only difference is that get_fast_random is guaranteed not to block.

void get_fast_random_bytes(Bytes);
bool get_good_random_bytes(Bytes bytes, bool allow_wait = true, bool fallback_to_fast = true);

template<typename T>
inline T get_fast_random()
{
    T value;
    Bytes bytes { reinterpret_cast<u8*>(&value), sizeof(T) };
    get_fast_random_bytes(bytes);
    return value;
}

template<typename T>
inline T get_good_random()
{
    T value;
    Bytes bytes { reinterpret_cast<u8*>(&value), sizeof(T) };
    get_good_random_bytes(bytes);
    return value;
}

}
