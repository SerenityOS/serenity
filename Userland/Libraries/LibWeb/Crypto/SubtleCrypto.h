/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Crypto {

class SubtleCrypto final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SubtleCrypto, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<SubtleCrypto>> create(JS::Realm&);

    virtual ~SubtleCrypto() override;

    JS::NonnullGCPtr<JS::Promise> digest(String const& algorithm, JS::Handle<JS::Object> const& data);

private:
    explicit SubtleCrypto(JS::Realm&);
    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
