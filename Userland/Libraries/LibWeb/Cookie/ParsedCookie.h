/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibIPC/Forward.h>

namespace Web::Cookie {

struct ParsedCookie {
    String name;
    String value;
    Optional<Core::DateTime> expiry_time_from_expires_attribute {};
    Optional<Core::DateTime> expiry_time_from_max_age_attribute {};
    Optional<String> domain {};
    Optional<String> path {};
    bool secure_attribute_present { false };
    bool http_only_attribute_present { false };
};

Optional<ParsedCookie> parse_cookie(const String& cookie_string);

}

namespace IPC {

bool encode(IPC::Encoder&, const Web::Cookie::ParsedCookie&);
ErrorOr<void> decode(IPC::Decoder&, Web::Cookie::ParsedCookie&);

}
