/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Time.h>
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
    DeprecatedString creation_time_to_string() const;
    DeprecatedString last_access_time_to_string() const;
    DeprecatedString expiry_time_to_string() const;

    DeprecatedString name;
    DeprecatedString value;
    SameSite same_site;
    UnixDateTime creation_time {};
    UnixDateTime last_access_time {};
    UnixDateTime expiry_time {};
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
ErrorOr<void> encode(Encoder&, Web::Cookie::Cookie const&);

template<>
ErrorOr<Web::Cookie::Cookie> decode(Decoder&);

}
