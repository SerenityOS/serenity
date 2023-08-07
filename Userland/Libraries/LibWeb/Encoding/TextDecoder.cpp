/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Encoding/TextDecoder.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::Encoding {

WebIDL::ExceptionOr<JS::NonnullGCPtr<TextDecoder>> TextDecoder::construct_impl(JS::Realm& realm, DeprecatedFlyString encoding)
{
    auto& vm = realm.vm();

    auto decoder = TextCodec::decoder_for(encoding);
    if (!decoder.has_value())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, TRY_OR_THROW_OOM(vm, String::formatted("Invalid encoding {}", encoding)) };

    return MUST_OR_THROW_OOM(realm.heap().allocate<TextDecoder>(realm, realm, *decoder, move(encoding), false, false));
}

// https://encoding.spec.whatwg.org/#dom-textdecoder
TextDecoder::TextDecoder(JS::Realm& realm, TextCodec::Decoder& decoder, DeprecatedFlyString encoding, bool fatal, bool ignore_bom)
    : PlatformObject(realm)
    , m_decoder(decoder)
    , m_encoding(move(encoding))
    , m_fatal(fatal)
    , m_ignore_bom(ignore_bom)
{
}

TextDecoder::~TextDecoder() = default;

void TextDecoder::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TextDecoderPrototype>(realm, "TextDecoder"));
}

// https://encoding.spec.whatwg.org/#dom-textdecoder-decode
WebIDL::ExceptionOr<DeprecatedString> TextDecoder::decode(Optional<JS::Handle<JS::Object>> const& input) const
{
    if (!input.has_value())
        return TRY_OR_THROW_OOM(vm(), m_decoder.to_utf8({}));

    // FIXME: Implement the streaming stuff.

    auto data_buffer_or_error = WebIDL::get_buffer_source_copy(*input->cell());
    if (data_buffer_or_error.is_error())
        return WebIDL::OperationError::create(realm(), "Failed to copy bytes from ArrayBuffer");
    auto& data_buffer = data_buffer_or_error.value();
    return TRY_OR_THROW_OOM(vm(), m_decoder.to_utf8({ data_buffer.data(), data_buffer.size() }));
}

}
