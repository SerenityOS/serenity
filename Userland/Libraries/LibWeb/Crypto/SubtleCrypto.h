/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
// FIXME: Generate these from IDL
namespace Web::Bindings {

// https://w3c.github.io/webcrypto/#dfn-Algorithm
struct Algorithm {
    String name;
};

};

namespace Web::Crypto {

class SubtleCrypto final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SubtleCrypto, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SubtleCrypto);

    using SupportedAlgorithmsMap = HashMap<String, HashMap<String, String, AK::ASCIICaseInsensitiveStringTraits>>;
    using AlgorithmIdentifier = Variant<JS::Handle<JS::Object>, String>;

public:
    [[nodiscard]] static JS::NonnullGCPtr<SubtleCrypto> create(JS::Realm&);

    virtual ~SubtleCrypto() override;

    JS::NonnullGCPtr<JS::Promise> digest(AlgorithmIdentifier const& algorithm, JS::Handle<WebIDL::BufferSource> const& data);

private:
    explicit SubtleCrypto(JS::Realm&);
    virtual void initialize(JS::Realm&) override;

    JS::ThrowCompletionOr<Bindings::Algorithm> normalize_an_algorithm(AlgorithmIdentifier const& algorithm, String operation);

    static SubtleCrypto::SupportedAlgorithmsMap& supported_algorithms_internal();
    static SubtleCrypto::SupportedAlgorithmsMap supported_algorithms();
    static void define_an_algorithm(String op, String algorithm, String type);
};

}
