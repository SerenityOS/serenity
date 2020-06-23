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

template<typename T>
class CTR : public Mode<T> {
public:
    constexpr static size_t IVSizeInBits = 128;

    virtual ~CTR() { }

    template<typename... Args>
    explicit constexpr CTR<T>(Args... args)
        : Mode<T>(args...)
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

    virtual Optional<ByteBuffer> encrypt(const ByteBuffer& in, ByteBuffer& out, Optional<ByteBuffer> ivec = {}) override
    {
        return this->encrypt_or_stream(&in, out, ivec);
    }

    Optional<ByteBuffer> key_stream(ByteBuffer& out, Optional<ByteBuffer> ivec = {})
    {
        return this->encrypt_or_stream(nullptr, out, ivec);
    }

    virtual void decrypt(const ByteBuffer& in, ByteBuffer& out, Optional<ByteBuffer> ivec = {}) override
    {
        (void)in;
        (void)out;
        (void)ivec;
        // FIXME: Implement CTR decryption when it is needed.
    }

private:
    static ByteBuffer increment(const ByteBuffer& in)
    {
        ByteBuffer new_buffer(in);
        size_t* num_view = (size_t*)new_buffer.data();

        for (size_t i = 0; i < in.size() / sizeof(size_t); ++i) {
            if (num_view[i] == (size_t)-1) {
                num_view[i] = 0;
            } else {
                num_view[i]++;
                break;
            }
        }
        return new_buffer;
    }

    Optional<ByteBuffer> encrypt_or_stream(const ByteBuffer* in, ByteBuffer& out, Optional<ByteBuffer> ivec)
    {
        size_t length;
        if (in) {
            ASSERT(in->size() <= out.size());
            length = in->size();
            if (length == 0)
                return {};
        } else {
            length = out.size();
        }

        auto& cipher = this->cipher();

        // FIXME: We should have two of these encrypt/decrypt functions that
        //        we SFINAE out based on whether the Cipher mode needs an ivec
        ASSERT(ivec.has_value());
        auto iv = ivec.value();

        typename T::BlockType block { cipher.padding_mode() };
        size_t offset { 0 };
        auto block_size = cipher.block_size();

        while (length > 0) {
            block.overwrite(iv.slice_view(0, block_size));

            cipher.encrypt_block(block, block);
            if (in) {
                block.apply_initialization_vector(in->data() + offset);
            }
            auto write_size = min(block_size, length);
            out.overwrite(offset, block.get().data(), write_size);

            iv = increment(iv);
            length -= write_size;
            offset += write_size;
        }

        return iv;
    }
};

}
}
