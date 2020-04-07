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

#include <LibCrypto/Cipher/Mode/Mode.h>

namespace Crypto {
namespace Cipher {

    template <typename T>
    class CBC : public Mode<T> {
    public:
        template <typename... Args>
        explicit constexpr CBC<T>(Args... args)
            : Mode<T>(args...)
        {
        }

        virtual Optional<ByteBuffer> encrypt(const ByteBuffer& in, ByteBuffer& out, Optional<ByteBuffer> ivec = {}) override
        {
            auto length = in.size();
            if (length == 0)
                return {};

            auto& cipher = this->cipher();

            // FIXME: We should have two of these encrypt/decrypt functions that
            //        we SFINAE out based on whether the Cipher mode needs an ivec
            ASSERT(ivec.has_value());
            const auto* iv = ivec.value().data();

            typename T::BlockType block { cipher.padding_mode() };
            size_t offset { 0 };
            auto block_size = cipher.block_size();

            while (length >= block_size) {
                block.overwrite(in.slice_view(offset, block_size));
                block.apply_initialization_vector(iv);
                cipher.encrypt_block(block, block);
                out.overwrite(offset, block.get().data(), block_size);
                iv = out.offset_pointer(offset);
                length -= block_size;
                offset += block_size;
            }

            if (length > 0) {
                block.overwrite(in.slice_view(offset, length));
                block.apply_initialization_vector(iv);
                cipher.encrypt_block(block, block);
                out.overwrite(offset, block.get().data(), block_size);
                iv = out.offset_pointer(offset);
            }

            return ByteBuffer::copy(iv, block_size);
        }
        virtual void decrypt(const ByteBuffer& in, ByteBuffer& out, Optional<ByteBuffer> ivec = {}) override
        {
            auto length = in.size();
            if (length == 0)
                return;

            auto& cipher = this->cipher();

            ASSERT(ivec.has_value());
            const auto* iv = ivec.value().data();

            auto block_size = cipher.block_size();

            // if the data is not aligned, it's not correct encrypted data
            // FIXME (ponder): Should we simply decrypt as much as we can?
            ASSERT(length % block_size == 0);

            typename T::BlockType block { cipher.padding_mode() };
            size_t offset { 0 };

            while (length > 0) {
                auto* slice = in.offset_pointer(offset);
                block.overwrite(slice, block_size);
                cipher.decrypt_block(block, block);
                block.apply_initialization_vector(iv);
                auto decrypted = block.get();
                out.overwrite(offset, decrypted.data(), decrypted.size());
                iv = slice;
                length -= block_size;
                offset += block_size;
            }
            this->prune_padding(out);
        }
    };

}

}
