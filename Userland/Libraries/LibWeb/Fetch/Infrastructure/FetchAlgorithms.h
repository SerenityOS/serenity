/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#fetch-elsewhere-fetch
class FetchAlgorithms : public JS::Cell {
    JS_CELL(FetchAlgorithms, JS::Cell);

public:
    struct ConsumeBodyFailureTag { };
    using BodyBytes = Variant<Empty, ConsumeBodyFailureTag, ByteBuffer>;

    using ProcessRequestBodyChunkLengthFunction = JS::SafeFunction<void(u64)>;
    using ProcessRequestEndOfBodyFunction = JS::SafeFunction<void()>;
    using ProcessEarlyHintsResponseFunction = JS::SafeFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseFunction = JS::SafeFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseEndOfBodyFunction = JS::SafeFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseConsumeBodyFunction = JS::SafeFunction<void(JS::NonnullGCPtr<Infrastructure::Response>, BodyBytes)>;

    struct Input {
        Optional<ProcessRequestBodyChunkLengthFunction> process_request_body_chunk_length;
        Optional<ProcessRequestEndOfBodyFunction> process_request_end_of_body;
        Optional<ProcessEarlyHintsResponseFunction> process_early_hints_response;
        Optional<ProcessResponseFunction> process_response;
        Optional<ProcessResponseEndOfBodyFunction> process_response_end_of_body;
        Optional<ProcessResponseConsumeBodyFunction> process_response_consume_body;
    };

    [[nodiscard]] static JS::NonnullGCPtr<FetchAlgorithms> create(JS::VM&, Input);

    Optional<ProcessRequestBodyChunkLengthFunction> const& process_request_body_chunk_length() const { return m_process_request_body_chunk_length; }
    Optional<ProcessRequestEndOfBodyFunction> const& process_request_end_of_body() const { return m_process_request_end_of_body; }
    Optional<ProcessEarlyHintsResponseFunction> const& process_early_hints_response() const { return m_process_early_hints_response; }
    Optional<ProcessResponseFunction> const& process_response() const { return m_process_response; }
    Optional<ProcessResponseEndOfBodyFunction> const& process_response_end_of_body() const { return m_process_response_end_of_body; }
    Optional<ProcessResponseConsumeBodyFunction> const& process_response_consume_body() const { return m_process_response_consume_body; }

private:
    explicit FetchAlgorithms(Input);

    Optional<ProcessRequestBodyChunkLengthFunction> m_process_request_body_chunk_length;
    Optional<ProcessRequestEndOfBodyFunction> m_process_request_end_of_body;
    Optional<ProcessEarlyHintsResponseFunction> m_process_early_hints_response;
    Optional<ProcessResponseFunction> m_process_response;
    Optional<ProcessResponseEndOfBodyFunction> m_process_response_end_of_body;
    Optional<ProcessResponseConsumeBodyFunction> m_process_response_consume_body;
};

}
