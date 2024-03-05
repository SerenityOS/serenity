/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibWeb/HTML/Origin.h>

namespace IPC {
template<>
ErrorOr<void> encode(Encoder& encoder, Web::HTML::Origin const& origin)
{
    TRY(encoder.encode<ByteString>(origin.scheme()));
    TRY(encoder.encode(origin.host()));
    TRY(encoder.encode(origin.port()));

    return {};
}

template<>
ErrorOr<Web::HTML::Origin> decode(Decoder& decoder)
{
    auto scheme = TRY(decoder.decode<ByteString>());
    auto host = TRY(decoder.decode<URL::Host>());
    u16 port = TRY(decoder.decode<u16>());

    return Web::HTML::Origin { move(scheme), move(host), port };
}

}
