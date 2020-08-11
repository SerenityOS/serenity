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

#include <AK/ByteBuffer.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <LibCrypto/Cipher/Cipher.h>

namespace Crypto {
namespace Cipher {

template<typename T>
class Mode {
public:
    virtual ~Mode() { }

    virtual void encrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}, Bytes* ivec_out = nullptr) = 0;
    virtual void decrypt(const ReadonlyBytes& in, Bytes& out, const Bytes& ivec = {}) = 0;

    virtual size_t IV_length() const = 0;

    const T& cipher() const { return m_cipher; }

    ByteBuffer create_aligned_buffer(size_t input_size) const
    {
        size_t remainder = (input_size + T::block_size()) % T::block_size();
        if (remainder == 0)
            return ByteBuffer::create_uninitialized(input_size);
        else
            return ByteBuffer::create_uninitialized(input_size + T::block_size() - remainder);
    }

    virtual String class_name() const = 0;
    T& cipher() { return m_cipher; }

protected:
    virtual void prune_padding(Bytes& data)
    {
        auto size = data.size();
        switch (m_cipher.padding_mode()) {
        case PaddingMode::CMS: {
            auto maybe_padding_length = data[size - 1];
            if (maybe_padding_length >= T::block_size()) {
                // cannot be padding (the entire block cannot be padding)
                return;
            }
            for (auto i = size - maybe_padding_length; i < size; ++i) {
                if (data[i] != maybe_padding_length) {
                    // not padding, part of data
                    return;
                }
            }
            data = data.slice(0, size - maybe_padding_length);
            break;
        }
        case PaddingMode::RFC5246: {
            auto maybe_padding_length = data[size - 1];
            // FIXME: If we want constant-time operations, this loop should not stop
            for (auto i = size - maybe_padding_length - 1; i < size; ++i) {
                if (data[i] != maybe_padding_length) {
                    // note that this is likely invalid padding
                    return;
                }
            }
            data = data.slice(0, size - maybe_padding_length - 1);
            break;
        }
        case PaddingMode::Null: {
            while (data[size - 1] == 0)
                --size;
            data = data.slice(0, size);
            break;
        }
        default:
            // FIXME: support other padding modes
            ASSERT_NOT_REACHED();
            break;
        }
    }

    // FIXME: Somehow add a reference version of this
    template<typename... Args>
    Mode(Args... args)
        : m_cipher(args...)
    {
    }

private:
    T m_cipher;
};
}

}
