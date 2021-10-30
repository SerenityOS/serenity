/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    PKSystem() = default;

    virtual void encrypt(ReadonlyBytes in, Bytes& out) = 0;
    virtual void decrypt(ReadonlyBytes in, Bytes& out) = 0;

    virtual void sign(ReadonlyBytes in, Bytes& out) = 0;
    virtual void verify(ReadonlyBytes in, Bytes& out) = 0;

    virtual String class_name() const = 0;

    virtual size_t output_size() const = 0;

protected:
    virtual ~PKSystem() = default;

    PublicKeyType m_public_key;
    PrivateKeyType m_private_key;
};

}
}
