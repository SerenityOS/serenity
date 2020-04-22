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
#include <AK/String.h>

namespace Crypto {
namespace PK {

// FIXME: Fixing name up for grabs
template<typename PrivKeyT, typename PubKeyT>
class PKSystem {
public:
    using PublicKeyType = PubKeyT;
    using PrivateKeyType = PrivKeyT;

    PKSystem(PublicKeyType& pubkey, PrivateKeyType& privkey)
        : m_public_key(pubkey)
        , m_private_key(privkey)
    {
    }

    PKSystem()
    {
    }

    virtual void encrypt(const ByteBuffer& in, ByteBuffer& out) = 0;
    virtual void decrypt(const ByteBuffer& in, ByteBuffer& out) = 0;

    virtual void sign(const ByteBuffer& in, ByteBuffer& out) = 0;
    virtual void verify(const ByteBuffer& in, ByteBuffer& out) = 0;

    virtual String class_name() const = 0;

    virtual size_t output_size() const = 0;

protected:
    PublicKeyType m_public_key;
    PrivateKeyType m_private_key;
};

}
}
