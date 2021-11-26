/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Format.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibCrypto/PK/Code/Code.h>

namespace Crypto {
namespace PK {

template<typename HashFunction, size_t SaltSize>
class EMSA_PSS : public Code<HashFunction> {
public:
    template<typename... Args>
    EMSA_PSS(Args... args)
        : Code<HashFunction>(args...)
    {
        m_buffer = Bytes { m_data_buffer, sizeof(m_data_buffer) };
    }

    static constexpr auto SaltLength = SaltSize;

    virtual void encode(ReadonlyBytes in, ByteBuffer& out, size_t em_bits) override
    {
        // FIXME: we're supposed to check if in.size() > HashFunction::input_limitation
        //        however, all of our current hash functions can hash unlimited blocks
        auto& hash_fn = this->hasher();
        hash_fn.update(in);
        auto message_hash = hash_fn.digest();
        constexpr auto hash_length = HashFunction::DigestSize;
        auto em_length = (em_bits + 7) / 8;
        u8 salt[SaltLength];

        fill_with_random(salt, SaltLength);

        if (em_length < hash_length + SaltLength + 2) {
            dbgln("Ooops...encoding error");
            return;
        }

        constexpr Array<u8, 8> zeros {};

        m_buffer.overwrite(0, zeros.data(), 8);
        m_buffer.overwrite(8, message_hash.data, HashFunction::DigestSize);
        m_buffer.overwrite(8 + HashFunction::DigestSize, salt, SaltLength);

        hash_fn.update(m_buffer);
        auto hash = hash_fn.digest();

        Vector<u8, 256> DB_data;
        DB_data.resize(em_length - HashFunction::DigestSize - 1);
        Bytes DB = DB_data;
        auto DB_offset = 0;

        for (size_t i = 0; i < em_length - SaltLength - HashFunction::DigestSize - 2; ++i)
            DB[DB_offset++] = 0;

        DB[DB_offset++] = 0x01;

        DB.overwrite(DB_offset, salt, SaltLength);

        auto mask_length = em_length - HashFunction::DigestSize - 1;

        Vector<u8, 256> DB_mask;
        DB_mask.resize(mask_length);
        Bytes DB_mask_buffer { DB_mask };
        // FIXME: we should probably allow reading from u8*
        MGF1(ReadonlyBytes { hash.data, HashFunction::DigestSize }, mask_length, DB_mask_buffer);

        for (size_t i = 0; i < DB.size(); ++i)
            DB_data[i] ^= DB_mask[i];

        auto count = (8 - (em_length * 8 - em_bits));
        DB_data[0] &= (0xff >> count) << count;

        out.overwrite(0, DB.data(), DB.size());
        out.overwrite(DB.size(), hash.data, hash_fn.DigestSize);
        out[DB.size() + hash_fn.DigestSize] = 0xbc;
    }

    virtual VerificationConsistency verify(ReadonlyBytes msg, ReadonlyBytes emsg, size_t em_bits) override
    {
        auto& hash_fn = this->hasher();
        hash_fn.update(msg);
        auto message_hash = hash_fn.digest();

        if (emsg.size() < HashFunction::DigestSize + SaltLength + 2)
            return VerificationConsistency::Inconsistent;

        if (emsg[emsg.size() - 1] != 0xbc)
            return VerificationConsistency::Inconsistent;

        auto mask_length = emsg.size() - HashFunction::DigestSize - 1;
        auto masked_DB = emsg.slice(0, mask_length);
        auto H = emsg.slice(mask_length, HashFunction::DigestSize);

        auto length_to_check = 8 * emsg.size() - em_bits;
        auto octet = masked_DB[0];
        for (size_t i = 0; i < length_to_check; ++i)
            if ((octet >> (8 - i)) & 0x01)
                return VerificationConsistency::Inconsistent;

        Vector<u8, 256> DB_mask;
        DB_mask.resize(mask_length);
        Bytes DB_mask_buffer { DB_mask };
        MGF1(H, mask_length, DB_mask_buffer);

        Vector<u8, 256> DB;
        DB.resize(mask_length);

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

        auto* salt = DB.span().offset(mask_length - SaltLength);
        u8 m_prime[8 + HashFunction::DigestSize + SaltLength] { 0 };

        auto m_prime_buffer = Bytes { m_prime, sizeof(m_prime) };

        m_prime_buffer.overwrite(8, message_hash.data, HashFunction::DigestSize);
        m_prime_buffer.overwrite(8 + HashFunction::DigestSize, salt, SaltLength);

        hash_fn.update(m_prime_buffer);
        auto H_prime = hash_fn.digest();

        if (__builtin_memcmp(message_hash.data, H_prime.data, HashFunction::DigestSize) != 0)
            return VerificationConsistency::Inconsistent;

        return VerificationConsistency::Consistent;
    }

    void MGF1(ReadonlyBytes seed, size_t length, Bytes out)
    {
        auto& hash_fn = this->hasher();
        ByteBuffer T;
        for (size_t counter = 0; counter < length / HashFunction::DigestSize - 1; ++counter) {
            hash_fn.update(seed);
            hash_fn.update((u8*)&counter, 4);
            if (auto result = T.try_append(hash_fn.digest().data, HashFunction::DigestSize); result.is_error()) {
                dbgln("EMSA_PSS: MGF1 digest failed: {}", result.error());
                return;
            }
        }
        out.overwrite(0, T.data(), length);
    }

private:
    u8 m_data_buffer[8 + HashFunction::DigestSize + SaltLength];
    Bytes m_buffer;
};

}
}
