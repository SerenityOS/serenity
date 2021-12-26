/*
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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCrypto/Cipher/Mode/Mode.h>

namespace Crypto {
namespace Cipher {

/*
 * Heads up: CTR is a *family* of modes, because the "counter" function is
 * implementation-defined. This makes interoperability a pain in the neurons.
 * Here are several contradicting(!) interpretations:
 *
 * "The counter can be *any function* which produces a sequence which is
 * guaranteed not to repeat for a long time, although an actual increment-by-one
 * counter is the simplest and most popular."
 * The illustrations show that first increment should happen *after* the first
 * round. I call this variant BIGINT_INCR_0.
 * The AESAVS goes a step further and requires only that "counters" do not
 * repeat, leaving the method of counting completely open.
 * See: https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Counter_(CTR)
 * See: https://csrc.nist.gov/csrc/media/projects/cryptographic-algorithm-validation-program/documents/aes/aesavs.pdf
 *
 * BIGINT_INCR_0 is the behavior of the OpenSSL command "openssl enc -aes-128-ctr",
 * and the behavior of CRYPTO_ctr128_encrypt(). OpenSSL is not alone in the
 * assumption that BIGINT_INCR_0 is all there is; even some NIST
 * specification/survey(?) doesn't consider counting any other way.
 * See: https://github.com/openssl/openssl/blob/33388b44b67145af2181b1e9528c381c8ea0d1b6/crypto/modes/ctr128.c#L71
 * See: http://www.cryptogrium.com/aes-ctr.html
 * See: https://web.archive.org/web/20150226072817/http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/ctr/ctr-spec.pdf
 *
 * "[T]he successive counter blocks are derived by applying an incrementing
 * function."
 * It defines a *family* of functions called "Standard Incrementing Function"
 * which only increment the lower-m bits, for some number 0<m<=blocksize.
 * The included test vectors suggest that the first increment should happen
 * *after* the first round. I call this INT32_INCR_0, or in general INTm_INCR_0.
 * This in particular is the behavior of CRYPTO_ctr128_encrypt_ctr32() in OpenSSL.
 * See: https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38a.pdf
 * See: https://github.com/openssl/openssl/blob/33388b44b67145af2181b1e9528c381c8ea0d1b6/crypto/modes/ctr128.c#L147
 *
 * The python package "cryptography" and RFC 3686 (which appears among the
 * first online search results when searching for "AES CTR 128 test vector")
 * share a peculiar interpretation of CTR mode: the counter is incremented *before*
 * the first round. RFC 3686 does not consider any other interpretation. I call
 * this variant BIGINT_INCR_1.
 * See: https://tools.ietf.org/html/rfc3686.html#section-6
 * See: https://cryptography.io/en/latest/development/test-vectors/#symmetric-ciphers
 *
 * And finally, because the method is left open, a different increment could be
 * used, for example little endian, or host endian, or mixed endian. Or any crazy
 * LSFR with sufficiently large period. That is the reason for the constant part
 * "INCR" in the previous counters.
 *
 * Due to this plethora of mutually-incompatible counters,
 * the method of counting should be a template parameter.
 * This currently implements BIGINT_INCR_0, which means perfect
 * interoperability with openssl. The test vectors from RFC 3686 just need to be
 * incremented by 1.
 * TODO: Implement other counters?
 */

template<typename T>
class CTR : public Mode<T> {
public:
    constexpr static size_t IVSizeInBits = 128;

    virtual ~CTR() { }

    // Must intercept `Intent`, because AES must always be set to
    // Encryption, even when decrypting AES-CTR.
    // TODO: How to deal with ciphers that take different arguments?
    template<typename KeyType, typename... Args>
    explicit constexpr CTR<T>(const KeyType& user_key, size_t key_bits, Intent = Intent::Encryption, Args... args)
        : Mode<T>(user_key, key_bits, args...)
    {
    }

    virtual String class_name() const override
    {
        StringBuilder builder;
        builder.append(this->cipher().class_name());
        builder.append("_CTR");
        return builder.build();
    }

    virtual size_t IV_length() const override { return IVSizeInBits / 8; }

    virtual void encrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}, Bytes* ivec_out = nullptr) override
    {
        // Our interpretation of "ivec" is what AES-CTR
        // would define as nonce + IV + 4 zero bytes.
        this->encrypt_or_stream(&in, out, ivec, ivec_out);
    }

    void key_stream(Bytes& out, const Bytes& ivec = {}, Bytes* ivec_out = nullptr)
    {
        this->encrypt_or_stream(nullptr, out, ivec, ivec_out);
    }

    virtual void decrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}) override
    {
        // XOR (and thus CTR) is the most symmetric mode.
        this->encrypt(in, out, ivec);
    }

private:
    u8 m_ivec_storage[IVSizeInBits / 8];
    typename T::BlockType m_cipher_block {};

    static void increment_inplace(Bytes& in)
    {
        for (size_t i = in.size(); i > 0;) {
            --i;
            if (in[i] == (u8)-1) {
                in[i] = 0;
            } else {
                in[i]++;
                break;
            }
        }
    }

    void encrypt_or_stream(const ReadonlyBytes* in, Bytes& out, const Bytes& ivec, Bytes* ivec_out = nullptr)
    {
        size_t length;
        if (in) {
            ASSERT(in->size() <= out.size());
            length = in->size();
            if (length == 0)
                return;
        } else {
            length = out.size();
        }

        auto& cipher = this->cipher();

        // FIXME: We should have two of these encrypt/decrypt functions that
        //        we SFINAE out based on whether the Cipher mode needs an ivec
        ASSERT(!ivec.is_empty());
        ASSERT(ivec.size() >= IV_length());

        m_cipher_block.set_padding_mode(cipher.padding_mode());

        __builtin_memcpy(m_ivec_storage, ivec.data(), IV_length());
        Bytes iv { m_ivec_storage, IV_length() };

        size_t offset { 0 };
        auto block_size = cipher.block_size();

        while (length > 0) {
            m_cipher_block.overwrite(iv.slice(0, block_size));

            cipher.encrypt_block(m_cipher_block, m_cipher_block);
            if (in) {
                m_cipher_block.apply_initialization_vector(in->data() + offset);
            }
            auto write_size = min(block_size, length);

            ASSERT(offset + write_size <= out.size());
            __builtin_memcpy(out.offset(offset), m_cipher_block.get().data(), write_size);

            increment_inplace(iv);
            length -= write_size;
            offset += write_size;
        }

        if (ivec_out)
            __builtin_memcpy(ivec_out->data(), iv.data(), min(ivec_out->size(), IV_length()));
    }
};

}
}
