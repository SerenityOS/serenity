/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
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
    String name;
    String value;
    SameSite same_site;
    Core::DateTime creation_time {};
    Core::DateTime last_access_time {};
    Core::DateTime expiry_time {};
    String domain {};
    String path {};
    bool secure { false };
    bool http_only { false };
    bool host_only { false };
    bool persistent { false };
};

StringView same_site_to_string(SameSite same_site_mode);

}

namespace IPC {

bool encode(Encoder&, Web::Cookie::Cookie const&);
ErrorOr<void> decode(Decoder&, Web::Cookie::Cookie&);

}
