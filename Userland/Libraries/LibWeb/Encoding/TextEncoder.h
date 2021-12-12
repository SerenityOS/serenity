/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#textencoder
class TextEncoder
    : public RefCounted<TextEncoder>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::TextEncoderWrapper;

    static NonnullRefPtr<TextEncoder> create()
    {
        return adopt_ref(*new TextEncoder());
    }

    static NonnullRefPtr<TextEncoder> create_with_global_object(Bindings::WindowObject&)
    {
        return TextEncoder::create();
    }

    JS::Uint8Array* encode(String const& input) const;

    static FlyString const& encoding();

protected:
    // https://encoding.spec.whatwg.org/#dom-textencoder
    TextEncoder() = default;
};

}
