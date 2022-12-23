/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibCore/DateTime.h>
#include <LibIPC/Forward.h>

namespace Web::Cookie {

enum class SameSite {
    Default,
    None,
    Strict,
    Lax
};

enum class Source {
    NonHttp,
    Http,
};

struct Cookie {
    DeprecatedString name;
    DeprecatedString value;
    SameSite same_site;
    Core::DateTime creation_time {};
    Core::DateTime last_access_time {};
    Core::DateTime expiry_time {};
    DeprecatedString domain {};
    DeprecatedString path {};
    bool secure { false };
    bool http_only { false };
    bool host_only { false };
    bool persistent { false };
};

StringView same_site_to_string(SameSite same_site_mode);
SameSite same_site_from_string(StringView same_site_mode);

}

namespace IPC {

template<>
bool encode(Encoder&, Web::Cookie::Cookie const&);

template<>
ErrorOr<Web::Cookie::Cookie> decode(Decoder&);

}
