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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCrypto/Cipher/Mode/Mode.h>

namespace Crypto {
namespace Cipher {

template<typename T>
class CBC : public Mode<T> {
public:
    constexpr static size_t IVSizeInBits = 128;

    virtual ~CBC() { }
    template<typename... Args>
    explicit constexpr CBC<T>(Args... args)
        : Mode<T>(args...)
    {
    }

    virtual String class_name() const override
    {
        StringBuilder builder;
        builder.append(this->cipher().class_name());
        builder.append("_CBC");
        return builder.build();
    }

    virtual size_t IV_length() const override { return IVSizeInBits / 8; }

    virtual void encrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}, Bytes* ivec_out = nullptr) override
    {
        auto length = in.size();
        if (length == 0)
            return;

        auto& cipher = this->cipher();

        // FIXME: We should have two of these encrypt/decrypt functions that
        //        we SFINAE out based on whether the Cipher mode needs an ivec
        ASSERT(!ivec.is_empty());
        const auto* iv = ivec.data();

        m_cipher_block.set_padding_mode(cipher.padding_mode());
        size_t offset { 0 };
        auto block_size = cipher.block_size();

        while (length >= block_size) {
            m_cipher_block.overwrite(in.slice(offset, block_size));
            m_cipher_block.apply_initialization_vector(iv);
            cipher.encrypt_block(m_cipher_block, m_cipher_block);
            ASSERT(offset + block_size <= out.size());
            __builtin_memcpy(out.offset(offset), m_cipher_block.get().data(), block_size);
            iv = out.offset(offset);
            length -= block_size;
            offset += block_size;
        }

        if (length > 0) {
            m_cipher_block.overwrite(in.slice(offset, length));
            m_cipher_block.apply_initialization_vector(iv);
            cipher.encrypt_block(m_cipher_block, m_cipher_block);
            ASSERT(offset + block_size <= out.size());
            __builtin_memcpy(out.offset(offset), m_cipher_block.get().data(), block_size);
            iv = out.offset(offset);
        }

        if (ivec_out)
            __builtin_memcpy(ivec_out->data(), iv, min(IV_length(), ivec_out->size()));
    }

    virtual void decrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}) override
    {
        auto length = in.size();
        if (length == 0)
            return;

        auto& cipher = this->cipher();

        ASSERT(!ivec.is_empty());
        const auto* iv = ivec.data();

        auto block_size = cipher.block_size();

        // if the data is not aligned, it's not correct encrypted data
        // FIXME (ponder): Should we simply decrypt as much as we can?
        ASSERT(length % block_size == 0);

        m_cipher_block.set_padding_mode(cipher.padding_mode());
        size_t offset { 0 };

        while (length > 0) {
            auto* slice = in.offset(offset);
            m_cipher_block.overwrite(slice, block_size);
            cipher.decrypt_block(m_cipher_block, m_cipher_block);
            m_cipher_block.apply_initialization_vector(iv);
            auto decrypted = m_cipher_block.get();
            ASSERT(offset + decrypted.size() <= out.size());
            __builtin_memcpy(out.offset(offset), decrypted.data(), decrypted.size());
            iv = slice;
            length -= block_size;
            offset += block_size;
        }
        out = out.slice(0, offset);
        this->prune_padding(out);
    }

private:
    typename T::BlockType m_cipher_block {};
};

}

}
