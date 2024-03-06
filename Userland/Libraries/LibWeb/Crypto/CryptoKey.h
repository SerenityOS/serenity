/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
    using InternalKeyData = Variant<ByteBuffer, Bindings::JsonWebKey>;

    [[nodiscard]] static JS::NonnullGCPtr<CryptoKey> create(JS::Realm&, InternalKeyData);

    virtual ~CryptoKey() override;

    bool extractable() const { return m_extractable; }
    Bindings::KeyType type() const { return m_type; }
    Object const* algorithm() const { return m_algorithm.ptr(); }
    Object const* usages() const { return m_usages.ptr(); }

    void set_extractable(bool extractable) { m_extractable = extractable; }
    void set_type(Bindings::KeyType type) { m_type = type; }
    void set_algorithm(JS::NonnullGCPtr<Object> algorithm) { m_algorithm = move(algorithm); }
    void set_usages(JS::NonnullGCPtr<Object> usages) { m_usages = move(usages); }

private:
    CryptoKey(JS::Realm&, InternalKeyData);
    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Visitor&) override;

    Bindings::KeyType m_type;
    bool m_extractable { false };
    JS::NonnullGCPtr<Object> m_algorithm;
    JS::NonnullGCPtr<Object> m_usages;

    InternalKeyData m_key_data;
};

}
