/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/ReadableByteStreamController.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamBYOBRequest.h>
#include <LibWeb/Streams/ReadableStreamDefaultReader.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#rbs-controller-desired-size
Optional<double> ReadableByteStreamController::desired_size() const
{
    // 1. Return ! ReadableByteStreamControllerGetDesiredSize(this).
    return readable_byte_stream_controller_get_desired_size(*this);
}

// https://streams.spec.whatwg.org/#rbs-controller-close
WebIDL::ExceptionOr<void> ReadableByteStreamController::close()
{
    // 1. If this.[[closeRequested]] is true, throw a TypeError exception.
    if (m_close_requested)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Controller is already closed"sv };

    // 2. If this.[[stream]].[[state]] is not "readable", throw a TypeError exception.
    if (m_stream->state() != ReadableStream::State::Readable) {
        auto message = m_stream->state() == ReadableStream::State::Closed ? "Cannot close a closed stream"sv : "Cannot close an errored stream"sv;
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, message };
    }

    // 3. Perform ? ReadableByteStreamControllerClose(this).
    TRY(readable_byte_stream_controller_close(*this));

    return {};
}

// https://streams.spec.whatwg.org/#rbs-controller-error
void ReadableByteStreamController::error(JS::Value error)
{
    // 1. Perform ! ReadableByteStreamControllerError(this, e).
    readable_byte_stream_controller_error(*this, error);
}

ReadableByteStreamController::ReadableByteStreamController(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

// https://streams.spec.whatwg.org/#rbs-controller-private-cancel
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> ReadableByteStreamController::cancel_steps(JS::Value reason)
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

// https://streams.spec.whatwg.org/#rbs-controller-private-pull
WebIDL::ExceptionOr<void> ReadableByteStreamController::pull_steps(JS::NonnullGCPtr<ReadRequest> read_request)
{
    auto& vm = this->vm();
    auto& realm = this->realm();

    // 1. Let stream be this.[[stream]].

    // 2. Assert: ! ReadableStreamHasDefaultReader(stream) is true.
    VERIFY(readable_stream_has_default_reader(*m_stream));

    // 3. If this.[[queueTotalSize]] > 0,
    if (m_queue_total_size > 0) {
        // 1. Assert: ! ReadableStreamGetNumReadRequests(stream) is 0.
        VERIFY(readable_stream_get_num_read_requests(*m_stream) == 0);

        // 2. Perform ! ReadableByteStreamControllerFillReadRequestFromQueue(this, readRequest).
        TRY(readable_byte_stream_controller_fill_read_request_from_queue(*this, read_request));
        // 3. Return.
        return {};
    }

    // 4. Let autoAllocateChunkSize be this.[[autoAllocateChunkSize]].

    // 5. If autoAllocateChunkSize is not undefined,
    if (m_auto_allocate_chunk_size.has_value()) {
        // 1. Let buffer be Construct(%ArrayBuffer%, « autoAllocateChunkSize »).
        auto buffer = JS::ArrayBuffer::create(realm, *m_auto_allocate_chunk_size);

        // 2. If buffer is an abrupt completion,
        if (buffer.is_throw_completion()) {
            // 1. Perform readRequest’s error steps, given buffer.[[Value]].
            read_request->on_error(*buffer.throw_completion().value());

            // 2. Return.
            return {};
        }

        // 3. Let pullIntoDescriptor be a new pull-into descriptor with buffer buffer.[[Value]], buffer byte length autoAllocateChunkSize, byte offset 0,
        //    byte length autoAllocateChunkSize, bytes filled 0, element size 1, view constructor %Uint8Array%, and reader type "default".
        PullIntoDescriptor pull_into_descriptor {
            .buffer = buffer.release_value(),
            .buffer_byte_length = *m_auto_allocate_chunk_size,
            .byte_offset = 0,
            .byte_length = *m_auto_allocate_chunk_size,
            .bytes_filled = 0,
            .element_size = 1,
            .view_constructor = *realm.intrinsics().uint8_array_constructor(),
            .reader_type = ReaderType::Default,
        };

        // 4. Append pullIntoDescriptor to this.[[pendingPullIntos]].
        TRY_OR_THROW_OOM(vm, m_pending_pull_intos.try_append(move(pull_into_descriptor)));
    }

    // 6. Perform ! ReadableStreamAddReadRequest(stream, readRequest).
    readable_stream_add_read_request(*m_stream, read_request);

    // 7. Perform ! ReadableByteStreamControllerCallPullIfNeeded(this).
    return readable_byte_stream_controller_call_pull_if_needed(*this);
}

// https://streams.spec.whatwg.org/#rbs-controller-private-pull
WebIDL::ExceptionOr<void> ReadableByteStreamController::release_steps()
{
    auto& vm = this->vm();

    // 1. If this.[[pendingPullIntos]] is not empty,
    if (!m_pending_pull_intos.is_empty()) {
        // 1. Let firstPendingPullInto be this.[[pendingPullIntos]][0].
        auto first_pending_pull_into = m_pending_pull_intos.first();

        // 2. Set firstPendingPullInto’s reader type to "none".
        first_pending_pull_into.reader_type = ReaderType::None;

        // 3. Set this.[[pendingPullIntos]] to the list « firstPendingPullInto ».
        m_pending_pull_intos.clear();
        TRY_OR_THROW_OOM(vm, m_pending_pull_intos.try_append(first_pending_pull_into));
    }

    return {};
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
