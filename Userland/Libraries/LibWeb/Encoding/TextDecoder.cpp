/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Encoding/TextDecoder.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::Encoding {

WebIDL::ExceptionOr<JS::NonnullGCPtr<TextDecoder>> TextDecoder::construct_impl(JS::Realm& realm, FlyString encoding)
{
    auto decoder = TextCodec::decoder_for(encoding);
    if (!decoder)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, String::formatted("Invalid encoding {}", encoding) };

    return JS::NonnullGCPtr(*realm.heap().allocate<TextDecoder>(realm, realm, *decoder, move(encoding), false, false));
}

// https://encoding.spec.whatwg.org/#dom-textdecoder
TextDecoder::TextDecoder(JS::Realm& realm, TextCodec::Decoder& decoder, FlyString encoding, bool fatal, bool ignore_bom)
    : PlatformObject(realm)
    , m_decoder(decoder)
    , m_encoding(move(encoding))
    , m_fatal(fatal)
    , m_ignore_bom(ignore_bom)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "TextDecoder"));
}

TextDecoder::~TextDecoder() = default;

// https://encoding.spec.whatwg.org/#dom-textdecoder-decode
WebIDL::ExceptionOr<String> TextDecoder::decode(JS::Handle<JS::Object> const& input) const
{
    // FIXME: Implement the streaming stuff.

    auto data_buffer_or_error = WebIDL::get_buffer_source_copy(*input.cell());
    if (data_buffer_or_error.is_error())
        return WebIDL::OperationError::create(realm(), "Failed to copy bytes from ArrayBuffer");
    auto& data_buffer = data_buffer_or_error.value();
    return m_decoder.to_utf8({ data_buffer.data(), data_buffer.size() });
}

}
