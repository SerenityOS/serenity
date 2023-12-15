/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Bindings/SubtleCryptoPrototype.h>
#include <LibWeb/Crypto/CryptoBindings.h>
#include <LibWeb/Crypto/CryptoKey.h>

namespace Web::Crypto {

class SubtleCrypto final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SubtleCrypto, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SubtleCrypto);

    using SupportedAlgorithmsMap = HashMap<String, HashMap<String, String, AK::ASCIICaseInsensitiveStringTraits>>;
    using KeyDataType = Variant<JS::Handle<WebIDL::BufferSource>, Bindings::JsonWebKey>;
    using AlgorithmIdentifier = Variant<JS::Handle<JS::Object>, String>;

public:
    [[nodiscard]] static JS::NonnullGCPtr<SubtleCrypto> create(JS::Realm&);

    virtual ~SubtleCrypto() override;

    JS::NonnullGCPtr<JS::Promise> digest(AlgorithmIdentifier const& algorithm, JS::Handle<WebIDL::BufferSource> const& data);
    JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> import_key(Bindings::KeyFormat format, KeyDataType keyData, AlgorithmIdentifier algorithm, bool extractable, Vector<Bindings::KeyUsage> keyUsages);

private:
    explicit SubtleCrypto(JS::Realm&);
    virtual void initialize(JS::Realm&) override;

    JS::ThrowCompletionOr<Bindings::Algorithm> normalize_an_algorithm(AlgorithmIdentifier const& algorithm, String operation);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<CryptoKey>> pbkdf2_import_key(Variant<ByteBuffer, Bindings::JsonWebKey, Empty> key_data, AlgorithmIdentifier algorithm, Bindings::KeyFormat format, bool extractable, Vector<Bindings::KeyUsage> usages);

    static SubtleCrypto::SupportedAlgorithmsMap& supported_algorithms_internal();
    static SubtleCrypto::SupportedAlgorithmsMap supported_algorithms();
    static void define_an_algorithm(String op, String algorithm, String type);
};

}
