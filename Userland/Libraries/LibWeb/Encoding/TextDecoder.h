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
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#textdecoder
class TextDecoder : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(TextDecoder, Bindings::PlatformObject);

public:
    static DOM::ExceptionOr<JS::NonnullGCPtr<TextDecoder>> create_with_global_object(HTML::Window&, FlyString encoding);

    virtual ~TextDecoder() override;

    DOM::ExceptionOr<String> decode(JS::Handle<JS::Object> const&) const;

    FlyString const& encoding() const { return m_encoding; }
    bool fatal() const { return m_fatal; }
    bool ignore_bom() const { return m_ignore_bom; };

private:
    // https://encoding.spec.whatwg.org/#dom-textdecoder
    TextDecoder(HTML::Window&, TextCodec::Decoder&, FlyString encoding, bool fatal, bool ignore_bom);

    TextCodec::Decoder& m_decoder;
    FlyString m_encoding;
    bool m_fatal { false };
    bool m_ignore_bom { false };
};

}

WRAPPER_HACK(TextDecoder, Web::Encoding)
