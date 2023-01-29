/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cookie.h"
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Web::Cookie {

StringView same_site_to_string(SameSite same_site)
{
    switch (same_site) {
    case SameSite::Default:
        return "Default"sv;
    case SameSite::None:
        return "None"sv;
    case SameSite::Lax:
        return "Lax"sv;
    case SameSite::Strict:
        return "Strict"sv;
    }
    VERIFY_NOT_REACHED();
}

SameSite same_site_from_string(StringView same_site_mode)
{
    if (same_site_mode.equals_ignoring_case("None"sv))
        return SameSite::None;
    if (same_site_mode.equals_ignoring_case("Strict"sv))
        return SameSite::Strict;
    if (same_site_mode.equals_ignoring_case("Lax"sv))
        return SameSite::Lax;
    return SameSite::Default;
}

}

template<>
ErrorOr<void> IPC::encode(Encoder& encoder, Web::Cookie::Cookie const& cookie)
{
    TRY(encoder.encode(cookie.name));
    TRY(encoder.encode(cookie.value));
    TRY(encoder.encode(cookie.domain));
    TRY(encoder.encode(cookie.path));
    TRY(encoder.encode(cookie.creation_time));
    TRY(encoder.encode(cookie.expiry_time));
    TRY(encoder.encode(cookie.host_only));
    TRY(encoder.encode(cookie.http_only));
    TRY(encoder.encode(cookie.last_access_time));
    TRY(encoder.encode(cookie.persistent));
    TRY(encoder.encode(cookie.secure));
    TRY(encoder.encode(cookie.same_site));

    return {};
}

template<>
ErrorOr<Web::Cookie::Cookie> IPC::decode(Decoder& decoder)
{
    auto name = TRY(decoder.decode<DeprecatedString>());
    auto value = TRY(decoder.decode<DeprecatedString>());
    auto domain = TRY(decoder.decode<DeprecatedString>());
    auto path = TRY(decoder.decode<DeprecatedString>());
    auto creation_time = TRY(decoder.decode<Core::DateTime>());
    auto expiry_time = TRY(decoder.decode<Core::DateTime>());
    auto host_only = TRY(decoder.decode<bool>());
    auto http_only = TRY(decoder.decode<bool>());
    auto last_access_time = TRY(decoder.decode<Core::DateTime>());
    auto persistent = TRY(decoder.decode<bool>());
    auto secure = TRY(decoder.decode<bool>());
    auto same_site = TRY(decoder.decode<Web::Cookie::SameSite>());

    return Web::Cookie::Cookie { move(name), move(value), same_site, move(creation_time), move(last_access_time), move(expiry_time), move(domain), move(path), secure, http_only, host_only, persistent };
}
