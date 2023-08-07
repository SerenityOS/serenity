/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <LibJS/Forward.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#textdecoder
class TextDecoder : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(TextDecoder, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TextDecoder>> construct_impl(JS::Realm&, DeprecatedFlyString encoding);

    virtual ~TextDecoder() override;

    WebIDL::ExceptionOr<DeprecatedString> decode(Optional<JS::Handle<JS::Object>> const&) const;

    DeprecatedFlyString const& encoding() const { return m_encoding; }
    bool fatal() const { return m_fatal; }
    bool ignore_bom() const { return m_ignore_bom; }

private:
    // https://encoding.spec.whatwg.org/#dom-textdecoder
    TextDecoder(JS::Realm&, TextCodec::Decoder&, DeprecatedFlyString encoding, bool fatal, bool ignore_bom);

    virtual void initialize(JS::Realm&) override;

    TextCodec::Decoder& m_decoder;
    DeprecatedFlyString m_encoding;
    bool m_fatal { false };
    bool m_ignore_bom { false };
};

}
