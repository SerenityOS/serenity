/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Heap/HeapFunction.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#fetch-elsewhere-fetch
class FetchAlgorithms : public JS::Cell {
    JS_CELL(FetchAlgorithms, JS::Cell);

public:
    struct ConsumeBodyFailureTag { };
    using BodyBytes = Variant<Empty, ConsumeBodyFailureTag, ByteBuffer>;

    using ProcessRequestBodyChunkLengthFunction = Function<void(u64)>;
    using ProcessRequestEndOfBodyFunction = Function<void()>;
    using ProcessEarlyHintsResponseFunction = Function<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseFunction = Function<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseEndOfBodyFunction = Function<void(JS::NonnullGCPtr<Infrastructure::Response>)>;
    using ProcessResponseConsumeBodyFunction = Function<void(JS::NonnullGCPtr<Infrastructure::Response>, BodyBytes)>;

    struct Input {
        ProcessRequestBodyChunkLengthFunction process_request_body_chunk_length;
        ProcessRequestEndOfBodyFunction process_request_end_of_body;
        ProcessEarlyHintsResponseFunction process_early_hints_response;
        ProcessResponseFunction process_response;
        ProcessResponseEndOfBodyFunction process_response_end_of_body;
        ProcessResponseConsumeBodyFunction process_response_consume_body;
    };

    [[nodiscard]] static JS::NonnullGCPtr<FetchAlgorithms> create(JS::VM&, Input);

    ProcessRequestBodyChunkLengthFunction const& process_request_body_chunk_length() const { return m_process_request_body_chunk_length->function(); }
    ProcessRequestEndOfBodyFunction const& process_request_end_of_body() const { return m_process_request_end_of_body->function(); }
    ProcessEarlyHintsResponseFunction const& process_early_hints_response() const { return m_process_early_hints_response->function(); }
    ProcessResponseFunction const& process_response() const { return m_process_response->function(); }
    ProcessResponseEndOfBodyFunction const& process_response_end_of_body() const { return m_process_response_end_of_body->function(); }
    ProcessResponseConsumeBodyFunction const& process_response_consume_body() const { return m_process_response_consume_body->function(); }

    virtual void visit_edges(JS::Cell::Visitor&) override;

private:
    explicit FetchAlgorithms(JS::VM&, Input);

    JS::NonnullGCPtr<JS::HeapFunction<void(u64)>> m_process_request_body_chunk_length;
    JS::NonnullGCPtr<JS::HeapFunction<void()>> m_process_request_end_of_body;
    JS::NonnullGCPtr<JS::HeapFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>> m_process_early_hints_response;
    JS::NonnullGCPtr<JS::HeapFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>> m_process_response;
    JS::NonnullGCPtr<JS::HeapFunction<void(JS::NonnullGCPtr<Infrastructure::Response>)>> m_process_response_end_of_body;
    JS::NonnullGCPtr<JS::HeapFunction<void(JS::NonnullGCPtr<Infrastructure::Response>, BodyBytes)>> m_process_response_consume_body;
};

}
