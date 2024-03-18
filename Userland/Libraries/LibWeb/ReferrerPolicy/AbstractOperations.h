/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::ReferrerPolicy {

enum class OriginOnly {
    Yes,
    No,
};

Optional<URL::URL> determine_requests_referrer(Fetch::Infrastructure::Request const&);
Optional<URL::URL> strip_url_for_use_as_referrer(Optional<URL::URL>, OriginOnly origin_only = OriginOnly::No);

}
