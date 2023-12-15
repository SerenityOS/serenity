/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/WebIDL/Buffers.h>

// FIXME: Generate these from IDL
namespace Web::Bindings {

// https://w3c.github.io/webcrypto/#JsonWebKey-dictionary
struct RsaOtherPrimesInfo {
    Optional<String> r;
    Optional<String> d;
    Optional<String> t;
};

// https://w3c.github.io/webcrypto/#JsonWebKey-dictionary
struct JsonWebKey {
    Optional<String> kty;
    Optional<String> use;
    Optional<Vector<String>> key_ops;
    Optional<String> alg;
    Optional<bool> ext;
    Optional<String> crv;
    Optional<String> x;
    Optional<String> y;
    Optional<String> d;
    Optional<String> n;
    Optional<String> e;
    Optional<String> p;
    Optional<String> q;
    Optional<String> dp;
    Optional<String> dq;
    Optional<String> qi;
    Optional<Vector<RsaOtherPrimesInfo>> oth;
    Optional<String> k;
};

// https://w3c.github.io/webcrypto/#dfn-Algorithm
struct Algorithm {
    String name;
};

// https://w3c.github.io/webcrypto/#key-algorithm-dictionary
class KeyAlgorithm : public JS::Object {
    JS_OBJECT(KeyAlgorithm, Object);
    JS_DECLARE_ALLOCATOR(KeyAlgorithm);

public:
    static JS::NonnullGCPtr<KeyAlgorithm> create(JS::Realm&);
    virtual ~KeyAlgorithm() override = default;

    String const& name() const { return m_name; }
    void set_name(String name) { m_name = move(name); }

private:
    KeyAlgorithm(JS::Realm&);
    virtual void initialize(JS::Realm&) override;

    JS_DECLARE_NATIVE_FUNCTION(name_getter);

    String m_name;
};

// https://w3c.github.io/webcrypto/#pbkdf2-params
struct Pbkdf2Params {
    JS::Handle<WebIDL::BufferSource> salt;
    u32 iterations;
    Variant<JS::Handle<JS::Object>, String> hash;
};

};
