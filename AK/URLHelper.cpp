/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/StringBuilder.h>
#include <AK/URLHelper.h>

static HashMap<String, NonnullRefPtr<URLHelper>, CaseInsensitiveStringTraits> s_helpers;

namespace AK {

NonnullRefPtr<URLHelper> URLHelper::from_scheme(const String& scheme)
{
    if (s_helpers.is_empty()) {
        s_helpers.set("file", adopt(*new FileURLHelper()));
        s_helpers.set("data", adopt(*new DataURLHelper()));
        s_helpers.set("http", adopt(*new HttpURLHelper()));
        s_helpers.set("https", s_helpers.get("http").value());
    }

    auto it = s_helpers.find(scheme);
    if (it == s_helpers.end())
        return adopt(*new URLHelper());
    return it->value;
}

bool DataURLHelper::take_over_parsing(Badge<URL>, GenericLexer& lexer, URL& url)
{
    auto payload = adopt(*new URL::Payload());

    // FIXME: When unspecified, the MIME-type should be "text/plain;charset=US-ASCII", but we don't do charsets, yet
    auto mime_type = lexer.consume_until([](char c) { return c == ';' || c == ','; });
    payload->set_mime_type(mime_type.is_empty() ? "text/plain" : mime_type);

    while (lexer.consume_specific(';')) {
        auto parameter = lexer.consume_until([](char c) { return c == ';' || c == ','; });
        if (parameter == "base64") {
            payload->set_encoding(URL::Payload::Encoding::Base64);
            break;
        }
        // FIXME: Maybe do something with the consumed data parameter in the future
    }

    if (!lexer.consume_specific(','))
        return false;

    auto payload_data = lexer.consume_all();
    switch (payload->encoding()) {
        case URL::Payload::Encoding::UrlEncoded:
            payload->set_data(URL::decode(payload_data).to_byte_buffer());
            break;
        case URL::Payload::Encoding::Base64:
            payload->set_data(decode_base64(payload_data));
            break;
        default:
            ASSERT_NOT_REACHED();
    }

    url.set_payload(payload);

    return lexer.is_eof();
}

String DataURLHelper::take_over_serializing(Badge<URL>, StringBuilder& builder, const URL& url)
{
    builder.append(url.payload().mime_type());

    if (url.payload().encoding() == URL::Payload::Encoding::Base64)
        builder.append(";base64");

    builder.append(',');

    switch (url.payload().encoding()) {
        case URL::Payload::Encoding::UrlEncoded:
            builder.append(URL::encode(url.payload().data()));
            break;
        case URL::Payload::Encoding::Base64:
            builder.append(encode_base64(url.payload().data().span()));
            break;
        default:
            ASSERT_NOT_REACHED();
    }

    return builder.to_string();
}

}
