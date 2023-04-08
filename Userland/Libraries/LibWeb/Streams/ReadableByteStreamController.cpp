/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/ReadableByteStreamController.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamBYOBRequest.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#rbs-controller-desired-size
Optional<double> ReadableByteStreamController::desired_size() const
{
    // 1. Return ! ReadableByteStreamControllerGetDesiredSize(this).
    return readable_byte_stream_controller_get_desired_size(*this);
}

ReadableByteStreamController::ReadableByteStreamController(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

// https://streams.spec.whatwg.org/#rbs-controller-private-cancel
WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>> ReadableByteStreamController::cancel_steps(JS::Value reason)
{
    // 1. Perform ! ReadableByteStreamControllerClearPendingPullIntos(this).
    readable_byte_stream_controller_clear_pending_pull_intos(*this);

    // 2. Perform ! ResetQueue(this).
    reset_queue(*this);

    // 3. Let result be the result of performing this.[[cancelAlgorithm]], passing in reason.
    auto result = (*m_cancel_algorithm)(reason);

    // 4. Perform ! ReadableByteStreamControllerClearAlgorithms(this).
    readable_byte_stream_controller_clear_algorithms(*this);

    // 5. Return result.
    return result;
}

void ReadableByteStreamController::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_byob_request);
    for (auto const& pending_pull_into : m_pending_pull_intos) {
        visitor.visit(pending_pull_into.buffer);
        visitor.visit(pending_pull_into.view_constructor);
    }
    for (auto const& item : m_queue)
        visitor.visit(item.buffer);
    visitor.visit(m_stream);
}

}
