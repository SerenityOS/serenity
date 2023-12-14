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

namespace Web::Crypto {

class SubtleCrypto final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SubtleCrypto, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SubtleCrypto);

    using SupportedAlgorithmsMap = HashMap<String, HashMap<String, String, AK::ASCIICaseInsensitiveStringTraits>>;

public:
    struct Algorithm {
        String name;
    };

    [[nodiscard]] static JS::NonnullGCPtr<SubtleCrypto> create(JS::Realm&);

    virtual ~SubtleCrypto() override;

    JS::NonnullGCPtr<JS::Promise> digest(Variant<JS::Handle<JS::Object>, String> const& algorithm, JS::Handle<WebIDL::BufferSource> const& data);

private:
    explicit SubtleCrypto(JS::Realm&);
    virtual void initialize(JS::Realm&) override;

    JS::ThrowCompletionOr<Algorithm> normalize_an_algorithm(Variant<JS::Handle<JS::Object>, String> const& algorithm, String operation);

    static SubtleCrypto::SupportedAlgorithmsMap& supported_algorithms_internal();
    static SubtleCrypto::SupportedAlgorithmsMap supported_algorithms();
    static void define_an_algorithm(String op, String algorithm, String type);
};

}
