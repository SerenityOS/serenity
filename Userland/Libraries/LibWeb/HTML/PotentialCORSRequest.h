/*
 * Copyright (c) 2023, Srikavin Ramkumar <me@srikavin.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>

namespace Web::HTML {

enum class SameOriginFallbackFlag {
    No,
    Yes,
};

[[nodiscard]] JS::NonnullGCPtr<Fetch::Infrastructure::Request> create_potential_CORS_request(JS::VM&, const URL&, Optional<Fetch::Infrastructure::Request::Destination>, CORSSettingAttribute, SameOriginFallbackFlag = SameOriginFallbackFlag::No);

}
