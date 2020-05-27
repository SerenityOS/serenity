/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <AK/Random.h>
#include <LibCrypto/PK/Code/Code.h>

static constexpr u8 zeros[] { 0, 0, 0, 0, 0, 0, 0, 0 };

namespace Crypto {
namespace PK {

template<typename HashFunction, size_t SaltSize>
class EMSA_PSS : public Code<HashFunction> {
public:
    template<typename... Args>
    EMSA_PSS(Args... args)
        : Code<HashFunction>(args...)
    {
        m_buffer = ByteBuffer::wrap(m_data_buffer, sizeof(m_data_buffer));
    }

    static constexpr auto SaltLength = SaltSize;

    virtual void encode(const ByteBuffer& in, ByteBuffer& out, size_t em_bits) override
    {
        // FIXME: we're supposed to check if in.size() > HashFunction::input_limitation
        //        however, all of our current hash functions can hash unlimited blocks
        auto& hash_fn = this->hasher();
        hash_fn.update(in);
        auto message_hash = hash_fn.digest();
        auto hash_length = hash_fn.DigestSize;
        auto em_length = (em_bits + 7) / 8;
        u8 salt[SaltLength];

        AK::fill_with_random(salt, SaltLength);

        if (em_length < hash_length + SaltLength + 2) {
            dbg() << "Ooops...encoding error";
            return;
        }

        m_buffer.overwrite(0, zeros, 8);
        m_buffer.overwrite(8, message_hash.data, HashFunction::DigestSize);
        m_buffer.overwrite(8 + HashFunction::DigestSize, salt, SaltLength);

        hash_fn.update(m_buffer);
        auto hash = hash_fn.digest();

        u8 DB_data[em_length - HashFunction::DigestSize - 1];
        auto DB = ByteBuffer::wrap(DB_data, em_length - HashFunction::DigestSize - 1);
        auto DB_offset = 0;

        for (size_t i = 0; i < em_length - SaltLength - HashFunction::DigestSize - 2; ++i)
            DB[DB_offset++] = 0;

        DB[DB_offset++] = 0x01;

        DB.overwrite(DB_offset, salt, SaltLength);

        auto mask_length = em_length - HashFunction::DigestSize - 1;

        u8 DB_mask[mask_length];
        auto DB_mask_buffer = ByteBuffer::wrap(DB_mask, mask_length);
        // FIXME: we should probably allow reading from u8*
        auto hash_buffer = ByteBuffer::wrap(hash.data, HashFunction::DigestSize);
        MGF1(hash_buffer, mask_length, DB_mask_buffer);

        for (size_t i = 0; i < DB.size(); ++i)
            DB_data[i] ^= DB_mask[i];

        auto count = (8 - (em_length * 8 - em_bits));
        DB_data[0] &= (0xff >> count) << count;

        out.overwrite(0, DB.data(), DB.size());
        out.overwrite(DB.size(), hash.data, hash_fn.DigestSize);
        out[DB.size() + hash_fn.DigestSize] = 0xbc;
    }

    virtual VerificationConsistency verify(const ByteBuffer& msg, const ByteBuffer& emsg, size_t em_bits) override
    {
        auto& hash_fn = this->hasher();
        hash_fn.update(msg);
        auto message_hash = hash_fn.digest();

        if (emsg.size() < HashFunction::DigestSize + SaltLength + 2)
            return VerificationConsistency::Inconsistent;

        if (emsg[emsg.size() - 1] != 0xbc)
            return VerificationConsistency::Inconsistent;

        auto mask_length = emsg.size() - HashFunction::DigestSize - 1;
        auto masked_DB = emsg.slice_view(0, mask_length);
        auto H = emsg.slice_view(mask_length, HashFunction::DigestSize);

        auto length_to_check = 8 * emsg.size() - em_bits;
        auto octet = masked_DB[0];
        for (size_t i = 0; i < length_to_check; ++i)
            if ((octet >> (8 - i)) & 0x01)
                return VerificationConsistency::Inconsistent;

        u8 DB_mask[mask_length];
        auto DB_mask_buffer = ByteBuffer::wrap(DB_mask, mask_length);
        MGF1(H, mask_length, DB_mask_buffer);

        u8 DB[mask_length];

        for (size_t i = 0; i < mask_length; ++i)
            DB[i] = masked_DB[i] ^ DB_mask[i];

        DB[0] &= 0xff >> (8 - length_to_check);

        auto check_octets = emsg.size() - HashFunction::DigestSize - SaltLength - 2;
        for (size_t i = 0; i < check_octets; ++i) {
            if (DB[i])
                return VerificationConsistency::Inconsistent;
        }

        if (DB[check_octets + 1] != 0x01)
            return VerificationConsistency::Inconsistent;

        auto* salt = DB + mask_length - SaltLength;
        u8 m_prime[8 + HashFunction::DigestSize + SaltLength] { 0, 0, 0, 0, 0, 0, 0, 0 };

        auto m_prime_buffer = ByteBuffer::wrap(m_prime, sizeof(m_prime));

        m_prime_buffer.overwrite(8, message_hash.data, HashFunction::DigestSize);
        m_prime_buffer.overwrite(8 + HashFunction::DigestSize, salt, SaltLength);

        hash_fn.update(m_prime_buffer);
        auto H_prime = hash_fn.digest();

        if (__builtin_memcmp(message_hash.data, H_prime.data, HashFunction::DigestSize))
            return VerificationConsistency::Inconsistent;

        return VerificationConsistency::Consistent;
    }

    void MGF1(const ByteBuffer& seed, size_t length, ByteBuffer& out)
    {
        auto& hash_fn = this->hasher();
        ByteBuffer T = ByteBuffer::create_zeroed(0);
        for (size_t counter = 0; counter < length / HashFunction::DigestSize - 1; ++counter) {
            hash_fn.update(seed);
            hash_fn.update((u8*)&counter, 4);
            T.append(hash_fn.digest().data, HashFunction::DigestSize);
        }
        out.overwrite(0, T.data(), length);
    }

private:
    u8 m_data_buffer[8 + HashFunction::DigestSize + SaltLength];
    ByteBuffer m_buffer;
};

}
}
