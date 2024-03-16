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
#include <LibWeb/WebIDL/Buffers.h>

namespace Web::Encoding {

JS_DEFINE_ALLOCATOR(TextDecoder);

WebIDL::ExceptionOr<JS::NonnullGCPtr<TextDecoder>> TextDecoder::construct_impl(JS::Realm& realm, FlyString encoding, Optional<TextDecoderOptions> const& options)
{
    auto& vm = realm.vm();

    auto decoder = TextCodec::decoder_for(encoding);
    if (!decoder.has_value())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, TRY_OR_THROW_OOM(vm, String::formatted("Invalid encoding {}", encoding)) };

    return realm.heap().allocate<TextDecoder>(realm, realm, *decoder, move(encoding), options.value_or({}).fatal, options.value_or({}).ignore_bom);
}

// https://encoding.spec.whatwg.org/#dom-textdecoder
TextDecoder::TextDecoder(JS::Realm& realm, TextCodec::Decoder& decoder, FlyString encoding, bool fatal, bool ignore_bom)
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
    WEB_SET_PROTOTYPE_FOR_INTERFACE(TextDecoder);
}

// https://encoding.spec.whatwg.org/#dom-textdecoder-decode
WebIDL::ExceptionOr<String> TextDecoder::decode(Optional<JS::Handle<WebIDL::BufferSource>> const& input, Optional<TextDecodeOptions> const&) const
{
    if (!input.has_value())
        return TRY_OR_THROW_OOM(vm(), m_decoder.to_utf8({}));

    // FIXME: Implement the streaming stuff.
    auto data_buffer_or_error = WebIDL::get_buffer_source_copy(*input.value()->raw_object());
    if (data_buffer_or_error.is_error())
        return WebIDL::OperationError::create(realm(), "Failed to copy bytes from ArrayBuffer"_fly_string);
    auto& data_buffer = data_buffer_or_error.value();
    return TRY_OR_THROW_OOM(vm(), m_decoder.to_utf8({ data_buffer.data(), data_buffer.size() }));
}

}
