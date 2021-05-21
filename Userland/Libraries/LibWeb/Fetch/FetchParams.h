/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/Function.h>
#include <LibWeb/Fetch/LoadRequest.h>
#include <LibWeb/Fetch/Response.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#connection-timing-info
struct ConnectionTimingInfo {
    // These should technically all be DOMHighResTimeStamp, but DOMHighResTimeStamp is a typedef of double anyway.
    double domain_lookup_start_time { 0.0 };
    double domain_lookup_end_time { 0.0 };
    double connection_start_time { 0.0 };
    double connection_end_time { 0.0 };
    double secure_connection_start_time { 0.0 };

    ByteBuffer alpn_negotiated_protocol;

    // FIXME: Add the "clamp and coarsen connection timing info" algorithm
};

// https://fetch.spec.whatwg.org/#fetch-timing-info
struct FetchTimingInfo {
    // These should technically all be DOMHighResTimeStamp, but DOMHighResTimeStamp is a typedef of double anyway.
    double start_time { 0.0 };
    double redirect_start_time { 0.0 };
    double redirect_end_time { 0.0 };
    double post_redirect_start_time { 0.0 };
    double final_service_worker_start_time { 0.0 };
    double final_network_request_start_time { 0.0 };
    double final_network_response_start_time { 0.0 };
    double end_time { 0.0 };

    size_t encoded_body_size { 0 };
    size_t decoded_body_size { 0 };

    // FIXME: This should be nullable.
    ConnectionTimingInfo final_connection_timing_info;
};

using ProcessRequestBodyType = Function<void(size_t)>;
using ProcessRequestEndOfBodyType = Function<void()>;
using ProcessReponseType = Function<void(const Response&)>;
using ProcessResponseEndOfBodyType = Function<void(Result<Variant<Response, ByteBuffer>, bool>)>;
using ProcessResponseDoneType = Function<void(const Response&)>;

// https://fetch.spec.whatwg.org/#fetch-params
struct FetchParams {
    LoadRequest& request;

    const ProcessRequestBodyType& process_request_body;
    const ProcessRequestEndOfBodyType& process_request_end_of_body;
    const ProcessReponseType& process_response;
    const ProcessResponseEndOfBodyType& process_response_end_of_body;
    const ProcessResponseDoneType& process_response_done;

    // FIXME: task destination (default null) - Null, a global object, or a parallel queue.

    bool cross_origin_isolated_capability { false };

    FetchTimingInfo timing_info;
};

}
