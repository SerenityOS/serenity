/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>

namespace Web::Fetch::Infrastructure {

JS::NonnullGCPtr<FetchAlgorithms> FetchAlgorithms::create(JS::VM& vm, Input input)
{
    return vm.heap().allocate_without_realm<FetchAlgorithms>(move(input));
}

FetchAlgorithms::FetchAlgorithms(Input input)
    : m_process_request_body_chunk_length(move(input.process_request_body_chunk_length))
    , m_process_request_end_of_body(move(input.process_request_end_of_body))
    , m_process_early_hints_response(move(input.process_early_hints_response))
    , m_process_response(move(input.process_response))
    , m_process_response_end_of_body(move(input.process_response_end_of_body))
    , m_process_response_consume_body(move(input.process_response_consume_body))
{
}

}
