/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibJS/Forward.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::Encoding {

// https://encoding.spec.whatwg.org/#textdecoder
class TextDecoder
    : public RefCounted<TextDecoder>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::TextDecoderWrapper;

    static DOM::ExceptionOr<NonnullRefPtr<TextDecoder>> create(FlyString encoding)
    {
        auto decoder = TextCodec::decoder_for(encoding);
        if (!decoder)
            return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, String::formatted("Invalid encoding {}", encoding) };

        return adopt_ref(*new TextDecoder(*decoder, move(encoding), false, false));
    }

    static DOM::ExceptionOr<NonnullRefPtr<TextDecoder>> create_with_global_object(Bindings::WindowObject&, FlyString label)
    {
        return TextDecoder::create(move(label));
    }

    DOM::ExceptionOr<String> decode(JS::Handle<JS::Object> const&) const;

    FlyString const& encoding() const { return m_encoding; }
    bool fatal() const { return m_fatal; }
    bool ignore_bom() const { return m_ignore_bom; };

protected:
    // https://encoding.spec.whatwg.org/#dom-textdecoder
    TextDecoder(TextCodec::Decoder& decoder, FlyString encoding, bool fatal, bool ignore_bom)
        : m_decoder(decoder)
        , m_encoding(move(encoding))
        , m_fatal(fatal)
        , m_ignore_bom(ignore_bom)
    {
    }

    TextCodec::Decoder& m_decoder;
    FlyString m_encoding;
    bool m_fatal { false };
    bool m_ignore_bom { false };
};

}
