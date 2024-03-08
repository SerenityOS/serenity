/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCrypto/PK/RSA.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/CryptoKeyPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Crypto/CryptoBindings.h>

namespace Web::Crypto {

class CryptoKey final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(CryptoKey, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(CryptoKey);

public:
    using InternalKeyData = Variant<ByteBuffer, Bindings::JsonWebKey, ::Crypto::PK::RSAPublicKey<>, ::Crypto::PK::RSAPrivateKey<>>;

    [[nodiscard]] static JS::NonnullGCPtr<CryptoKey> create(JS::Realm&, InternalKeyData);

    virtual ~CryptoKey() override;

    bool extractable() const { return m_extractable; }
    Bindings::KeyType type() const { return m_type; }
    JS::Object const* algorithm() const { return m_algorithm; }
    JS::Object const* usages() const { return m_usages; }

    Vector<Bindings::KeyUsage> internal_usages() const { return m_key_usages; }

    void set_extractable(bool extractable) { m_extractable = extractable; }
    void set_type(Bindings::KeyType type) { m_type = type; }
    void set_algorithm(JS::NonnullGCPtr<Object> algorithm) { m_algorithm = move(algorithm); }
    void set_usages(Vector<Bindings::KeyUsage>);

private:
    CryptoKey(JS::Realm&, InternalKeyData);
    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Visitor&) override;

    Bindings::KeyType m_type;
    bool m_extractable { false };
    JS::NonnullGCPtr<Object> m_algorithm;
    JS::NonnullGCPtr<Object> m_usages;

    Vector<Bindings::KeyUsage> m_key_usages;
    InternalKeyData m_key_data;
};

// https://w3c.github.io/webcrypto/#ref-for-dfn-CryptoKeyPair-2
class CryptoKeyPair : public JS::Object {
    JS_OBJECT(CryptoKeyPair, Object);
    JS_DECLARE_ALLOCATOR(CryptoKeyPair);

public:
    static JS::NonnullGCPtr<CryptoKeyPair> create(JS::Realm&, JS::NonnullGCPtr<CryptoKey> public_key, JS::NonnullGCPtr<CryptoKey> private_key);
    virtual ~CryptoKeyPair() override = default;

    JS::NonnullGCPtr<CryptoKey> public_key() const { return m_public_key; }
    JS::NonnullGCPtr<CryptoKey> private_key() const { return m_private_key; }

private:
    CryptoKeyPair(JS::Realm&, JS::NonnullGCPtr<CryptoKey> public_key, JS::NonnullGCPtr<CryptoKey> private_key);
    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Visitor&) override;

    JS_DECLARE_NATIVE_FUNCTION(public_key_getter);
    JS_DECLARE_NATIVE_FUNCTION(private_key_getter);

    JS::NonnullGCPtr<CryptoKey> m_public_key;
    JS::NonnullGCPtr<CryptoKey> m_private_key;
};

}
