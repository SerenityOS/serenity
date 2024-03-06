/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/SubtleCryptoPrototype.h>
#include <LibWeb/Crypto/CryptoBindings.h>
#include <LibWeb/Crypto/CryptoKey.h>
#include <LibWeb/WebIDL/Buffers.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Crypto {

using KeyDataType = Variant<JS::Handle<WebIDL::BufferSource>, Bindings::JsonWebKey>;
using AlgorithmIdentifier = Variant<JS::Handle<JS::Object>, String>;
using HashAlgorithmIdentifier = AlgorithmIdentifier;

// https://w3c.github.io/webcrypto/#algorithm-overview

struct AlgorithmParams {
    String name;

    static JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> from_value(JS::VM&, JS::Value);
};

// https://w3c.github.io/webcrypto/#pbkdf2-params
struct PBKDF2Params : public AlgorithmParams {
    JS::Handle<WebIDL::BufferSource> salt;
    u32 iterations;
    HashAlgorithmIdentifier hash;

    static JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> from_value(JS::VM&, JS::Value);
};

class AlgorithmMethods {
public:
    virtual ~AlgorithmMethods();

    virtual WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> digest(AlgorithmParams const&, ByteBuffer const&)
    {
        return WebIDL::NotSupportedError::create(m_realm, "digest is not supported"_fly_string);
    }

    virtual WebIDL::ExceptionOr<JS::NonnullGCPtr<CryptoKey>> import_key(AlgorithmParams const&, Bindings::KeyFormat, CryptoKey::InternalKeyData, bool, Vector<Bindings::KeyUsage> const&)
    {
        return WebIDL::NotSupportedError::create(m_realm, "importKey is not supported"_fly_string);
    }

    static NonnullOwnPtr<AlgorithmMethods> create(JS::Realm& realm) { return adopt_own(*new AlgorithmMethods(realm)); }

protected:
    explicit AlgorithmMethods(JS::Realm& realm)
        : m_realm(realm)
    {
    }

    JS::Realm& m_realm;
};

class PBKDF2 : public AlgorithmMethods {
public:
    virtual WebIDL::ExceptionOr<JS::NonnullGCPtr<CryptoKey>> import_key(AlgorithmParams const&, Bindings::KeyFormat, CryptoKey::InternalKeyData, bool, Vector<Bindings::KeyUsage> const&) override;

    static NonnullOwnPtr<AlgorithmMethods> create(JS::Realm& realm) { return adopt_own(*new PBKDF2(realm)); }

private:
    explicit PBKDF2(JS::Realm& realm)
        : AlgorithmMethods(realm)
    {
    }
};

class SHA : public AlgorithmMethods {
public:
    virtual WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> digest(AlgorithmParams const&, ByteBuffer const&) override;

    static NonnullOwnPtr<AlgorithmMethods> create(JS::Realm& realm) { return adopt_own(*new SHA(realm)); }

private:
    explicit SHA(JS::Realm& realm)
        : AlgorithmMethods(realm)
    {
    }
};

}
