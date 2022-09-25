/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#readablestream
class ReadableStream final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Request, Bindings::PlatformObject);

public:
    enum class State {
        Readable,
        Closed,
        Errored,
    };

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStream>> create_with_global_object(HTML::Window&);

    virtual ~ReadableStream() override;

    JS::GCPtr<JS::Object> controller() const { return m_controller; }
    JS::GCPtr<JS::Object> reader() const { return m_reader; }
    JS::Value stored_error() const { return m_stored_error; }

    bool is_readable() const;
    bool is_closed() const;
    bool is_errored() const;
    bool is_locked() const;
    bool is_disturbed() const;

private:
    explicit ReadableStream(HTML::Window&);

    virtual void visit_edges(Cell::Visitor&) override;

    // https://streams.spec.whatwg.org/#readablestream-controller
    // A ReadableStreamDefaultController or ReadableByteStreamController created with the ability to control the state and queue of this stream
    JS::GCPtr<JS::Object> m_controller;

    // https://streams.spec.whatwg.org/#readablestream-detached
    // A boolean flag set to true when the stream is transferred
    bool m_detached { false };

    // https://streams.spec.whatwg.org/#readablestream-disturbed
    // A boolean flag set to true when the stream has been read from or canceled
    bool m_disturbed { false };

    // https://streams.spec.whatwg.org/#readablestream-reader
    // A ReadableStreamDefaultReader or ReadableStreamBYOBReader instance, if the stream is locked to a reader, or undefined if it is not
    JS::GCPtr<JS::Object> m_reader;

    // https://streams.spec.whatwg.org/#readablestream-state
    // A string containing the stream’s current state, used internally; one of "readable", "closed", or "errored"
    State m_state { State::Readable };

    // https://streams.spec.whatwg.org/#readablestream-storederror
    // A value indicating how the stream failed, to be given as a failure reason or exception when trying to operate on an errored stream
    JS::Value m_stored_error { JS::js_undefined() };
};

}
