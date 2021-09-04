/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/HttpRequest.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(uint8_t const* data, size_t size)
{
    auto request_wrapper = HTTP::HttpRequest::from_raw_request(ReadonlyBytes { data, size });
    if (!request_wrapper.has_value())
        return 1;

    auto& request = request_wrapper.value();
    VERIFY(request.method() != HTTP::HttpRequest::Method::Invalid);

    return 0;
}
