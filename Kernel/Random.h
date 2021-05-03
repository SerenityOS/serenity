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

#pragma once

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Lock.h>
#include <Kernel/StdLib.h>
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

    FortunaPRNG()
        : m_counter(ByteBuffer::create_zeroed(BlockType::block_size()))
    {
    }

    bool get_random_bytes(u8* buffer, size_t n)
    {
        ScopedSpinLock lock(m_lock);
        if (!is_ready())
            return false;
        if (m_p0_len >= reseed_threshold) {
            this->reseed();
        }

        VERIFY(is_seeded());

        // FIXME: More than 2^20 bytes cannot be generated without refreshing the key.
        VERIFY(n < (1 << 20));

        typename CipherType::CTRMode cipher(m_key, KeySize, Crypto::Cipher::Intent::Encryption);

        Bytes buffer_span { buffer, n };
        auto counter_span = m_counter.bytes();
        cipher.key_stream(buffer_span, counter_span, &counter_span);

        // Extract a new key from the prng stream.
        Bytes key_span = m_key.bytes();
        cipher.key_stream(key_span, counter_span, &counter_span);
        return true;
    }

    template<typename T>
    void add_random_event(const T& event_data, size_t pool)
    {
        pool %= pool_count;
        if (pool == 0) {
            m_p0_len++;
        }
        m_pools[pool].update(reinterpret_cast<const u8*>(&event_data), sizeof(T));
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

    SpinLock<u8>& get_lock() { return m_lock; }

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
        m_key = ByteBuffer::copy(digest.immutable_data(),
            digest.data_length());

        m_reseed_number++;
        m_p0_len = 0;
    }

    ByteBuffer m_counter;
    size_t m_reseed_number { 0 };
    size_t m_p0_len { 0 };
    ByteBuffer m_key;
    HashType m_pools[pool_count];
    SpinLock<u8> m_lock;
};

class KernelRng : public Lockable<FortunaPRNG<Crypto::Cipher::AESCipher, Crypto::Hash::SHA256, 256>> {
    AK_MAKE_ETERNAL;

public:
    KernelRng();
    static KernelRng& the();

    void wait_for_entropy();

    void wake_if_ready();

    SpinLock<u8>& get_lock() { return resource().get_lock(); }

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
    void add_random_event(const T& event_data)
    {
        auto& kernel_rng = KernelRng::the();
        ScopedSpinLock lock(kernel_rng.get_lock());
        // We don't lock this because on the off chance a pool is corrupted, entropy isn't lost.
        Event<T> event = { read_tsc(), m_source, event_data };
        kernel_rng.resource().add_random_event(event, m_pool);
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

void get_fast_random_bytes(u8*, size_t);
bool get_good_random_bytes(u8*, size_t, bool allow_wait = true, bool fallback_to_fast = true);

template<typename T>
inline T get_fast_random()
{
    T value;
    get_fast_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

template<typename T>
inline T get_good_random()
{
    T value;
    get_good_random_bytes(reinterpret_cast<u8*>(&value), sizeof(T));
    return value;
}

}
