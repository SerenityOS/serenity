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

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibCrypto/PK/PK.h>

namespace Crypto {
namespace PK {
template<typename Integer = u64>
class RSAPublicKey {
public:
    RSAPublicKey(const Integer& n, const Integer& e)
        : m_modulus(n)
        , m_public_exponent(e)
    {
    }

    RSAPublicKey()
        : m_modulus(0)
        , m_public_exponent(0)
    {
    }

    //--stuff it should do

    const Integer& modulus() const { return m_modulus; }
    const Integer& public_exponent() const { return m_public_exponent; }
    size_t length() const { return m_length; }
    void set_length(size_t length) { m_length = length; }

    void set(const Integer& n, const Integer& e)
    {
        m_modulus = n;
        m_public_exponent = e;
        m_length = (n.trimmed_length() * sizeof(u32));
    }

private:
    Integer m_modulus;
    Integer m_public_exponent;
    size_t m_length { 0 };
};

template<typename Integer = UnsignedBigInteger>
class RSAPrivateKey {
public:
    RSAPrivateKey(const Integer& n, const Integer& d, const Integer& e)
        : m_modulus(n)
        , m_private_exponent(d)
        , m_public_exponent(e)
    {
    }

    RSAPrivateKey()
    {
    }

    //--stuff it should do
    const Integer& modulus() const { return m_modulus; }
    const Integer& private_exponent() const { return m_private_exponent; }
    const Integer& public_exponent() const { return m_public_exponent; }
    size_t length() const { return m_length; }
    void set_length(size_t length) { m_length = length; }

    void set(const Integer& n, const Integer& d, const Integer& e)
    {
        m_modulus = n;
        m_private_exponent = d;
        m_public_exponent = e;
        m_length = (n.length() * sizeof(u32));
    }

private:
    Integer m_modulus;
    Integer m_private_exponent;
    Integer m_public_exponent;
    size_t m_length { 0 };
};

template<typename PubKey, typename PrivKey>
struct RSAKeyPair {
    PubKey public_key;
    PrivKey private_key;
};

using IntegerType = UnsignedBigInteger;
class RSA : public PKSystem<RSAPrivateKey<IntegerType>, RSAPublicKey<IntegerType>> {
    template<typename T>
    friend class RSA_EMSA_PSS;

public:
    using KeyPairType = RSAKeyPair<PublicKeyType, PrivateKeyType>;

    static KeyPairType parse_rsa_key(ReadonlyBytes);
    static KeyPairType generate_key_pair(size_t bits = 256)
    {
        IntegerType e { 65537 }; // :P
        IntegerType p, q;
        IntegerType lambda;

        do {
            p = NumberTheory::random_big_prime(bits / 2);
            q = NumberTheory::random_big_prime(bits / 2);
            lambda = NumberTheory::LCM(p.minus(1), q.minus(1));
            dbg() << "checking combination p=" << p << ", q=" << q << ", lambda=" << lambda.length();
        } while (!(NumberTheory::GCD(e, lambda) == 1));

        auto n = p.multiplied_by(q);

        auto d = NumberTheory::ModularInverse(e, lambda);
        dbg() << "Your keys are Pub{n=" << n << ", e=" << e << "} and Priv{n=" << n << ", d=" << d << "}";
        RSAKeyPair<PublicKeyType, PrivateKeyType> keys {
            { n, e },
            { n, d, e }
        };
        keys.public_key.set_length(bits / 2 / 8);
        keys.private_key.set_length(bits / 2 / 8);
        return keys;
    }

    RSA(IntegerType n, IntegerType d, IntegerType e)
    {
        m_public_key.set(n, e);
        m_private_key.set(n, d, e);
    }

    RSA(PublicKeyType& pubkey, PrivateKeyType& privkey)
        : PKSystem<RSAPrivateKey<IntegerType>, RSAPublicKey<IntegerType>>(pubkey, privkey)
    {
    }

    RSA(const ByteBuffer& publicKeyPEM, const ByteBuffer& privateKeyPEM)
    {
        import_public_key(publicKeyPEM);
        import_private_key(privateKeyPEM);
    }

    RSA(const StringView& privKeyPEM)
    {
        import_private_key(privKeyPEM.bytes());
        m_public_key.set(m_private_key.modulus(), m_private_key.public_exponent());
    }

    // create our own keys
    RSA()
    {
        auto pair = generate_key_pair();
        m_public_key = pair.public_key;
        m_private_key = pair.private_key;
    }

    virtual void encrypt(const ByteBuffer& in, ByteBuffer& out) override;
    virtual void decrypt(const ByteBuffer& in, ByteBuffer& out) override;

    virtual void sign(const ByteBuffer& in, ByteBuffer& out) override;
    virtual void verify(const ByteBuffer& in, ByteBuffer& out) override;

    virtual String class_name() const override { return "RSA"; }

    virtual size_t output_size() const override { return m_public_key.length(); }

    void import_public_key(ReadonlyBytes, bool pem = true);
    void import_private_key(ReadonlyBytes, bool pem = true);

    const PrivateKeyType& private_key() const { return m_private_key; }
    const PublicKeyType& public_key() const { return m_public_key; }
};

template<typename HashFunction>
class RSA_EMSA_PSS {
public:
    RSA_EMSA_PSS(RSA& rsa)
        : m_rsa(rsa)
    {
    }

    void sign(const ByteBuffer& in, ByteBuffer& out);
    VerificationConsistency verify(const ByteBuffer& in);

private:
    EMSA_PSS<HashFunction, HashFunction::DigestSize> m_emsa_pss;
    RSA m_rsa;
};

class RSA_PKCS1_EME : public RSA {
public:
    // forward all constructions to RSA
    template<typename... Args>
    RSA_PKCS1_EME(Args... args)
        : RSA(args...)
    {
    }

    ~RSA_PKCS1_EME() { }

    virtual void encrypt(const ByteBuffer& in, ByteBuffer& out) override;
    virtual void decrypt(const ByteBuffer& in, ByteBuffer& out) override;

    virtual void sign(const ByteBuffer&, ByteBuffer&) override;
    virtual void verify(const ByteBuffer&, ByteBuffer&) override;

    virtual String class_name() const override { return "RSA_PKCS1-EME"; }
    virtual size_t output_size() const override { return m_public_key.length(); }
};
}
}
