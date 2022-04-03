/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibCrypto/PK/PK.h>

namespace Crypto {
namespace PK {
template<typename Integer = UnsignedBigInteger>
class RSAPublicKey {
public:
    RSAPublicKey(Integer n, Integer e)
        : m_modulus(move(n))
        , m_public_exponent(move(e))
        , m_length(m_modulus.trimmed_length() * sizeof(u32))
    {
    }

    RSAPublicKey()
        : m_modulus(0)
        , m_public_exponent(0)
    {
    }

    Integer const& modulus() const { return m_modulus; }
    Integer const& public_exponent() const { return m_public_exponent; }
    size_t length() const { return m_length; }
    void set_length(size_t length) { m_length = length; }

    void set(Integer n, Integer e)
    {
        m_modulus = move(n);
        m_public_exponent = move(e);
        m_length = (m_modulus.trimmed_length() * sizeof(u32));
    }

private:
    Integer m_modulus;
    Integer m_public_exponent;
    size_t m_length { 0 };
};

template<typename Integer = UnsignedBigInteger>
class RSAPrivateKey {
public:
    RSAPrivateKey(Integer n, Integer d, Integer e)
        : m_modulus(move(n))
        , m_private_exponent(move(d))
        , m_public_exponent(move(e))
        , m_length(m_modulus.trimmed_length() * sizeof(u32))
    {
    }

    RSAPrivateKey() = default;

    Integer const& modulus() const { return m_modulus; }
    Integer const& private_exponent() const { return m_private_exponent; }
    Integer const& public_exponent() const { return m_public_exponent; }
    size_t length() const { return m_length; }
    void set_length(size_t length) { m_length = length; }

    void set(Integer n, Integer d, Integer e)
    {
        m_modulus = move(n);
        m_private_exponent = move(d);
        m_public_exponent = move(e);
        m_length = m_modulus.trimmed_length() * sizeof(u32);
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

    static KeyPairType parse_rsa_key(ReadonlyBytes der);
    static KeyPairType generate_key_pair(size_t bits = 256)
    {
        IntegerType e { 65537 }; // :P
        IntegerType p, q;
        IntegerType lambda;

        do {
            p = NumberTheory::random_big_prime(bits / 2);
            q = NumberTheory::random_big_prime(bits / 2);
            lambda = NumberTheory::LCM(p.minus(1), q.minus(1));
            dbgln("checking combination p={}, q={}, lambda={}", p, q, lambda.length());
        } while (!(NumberTheory::GCD(e, lambda) == 1));

        auto n = p.multiplied_by(q);

        auto d = NumberTheory::ModularInverse(e, lambda);
        dbgln("Your keys are Pub(n={}, e={}) and Priv(n={}, d={})", n, e, n, d);
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

    RSA(ByteBuffer const& publicKeyPEM, ByteBuffer const& privateKeyPEM)
    {
        import_public_key(publicKeyPEM);
        import_private_key(privateKeyPEM);
    }

    RSA(StringView privKeyPEM)
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

    virtual void encrypt(ReadonlyBytes in, Bytes& out) override;
    virtual void decrypt(ReadonlyBytes in, Bytes& out) override;

    virtual void sign(ReadonlyBytes in, Bytes& out) override;
    virtual void verify(ReadonlyBytes in, Bytes& out) override;

#ifndef KERNEL
    virtual String class_name() const override
    {
        return "RSA";
    }
#endif

    virtual size_t output_size() const override
    {
        return m_public_key.length();
    }

    void import_public_key(ReadonlyBytes, bool pem = true);
    void import_private_key(ReadonlyBytes, bool pem = true);

    PrivateKeyType const& private_key() const { return m_private_key; }
    PublicKeyType const& public_key() const { return m_public_key; }
};

template<typename HashFunction>
class RSA_EMSA_PSS {
public:
    RSA_EMSA_PSS(RSA& rsa)
        : m_rsa(rsa)
    {
    }

    void sign(ReadonlyBytes in, Bytes& out);
    VerificationConsistency verify(ReadonlyBytes in);

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

    ~RSA_PKCS1_EME() = default;

    virtual void encrypt(ReadonlyBytes in, Bytes& out) override;
    virtual void decrypt(ReadonlyBytes in, Bytes& out) override;

    virtual void sign(ReadonlyBytes, Bytes&) override;
    virtual void verify(ReadonlyBytes, Bytes&) override;

#ifndef KERNEL
    virtual String class_name() const override
    {
        return "RSA_PKCS1-EME";
    }
#endif
    virtual size_t output_size() const override
    {
        return m_public_key.length();
    }
};
}
}
