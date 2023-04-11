/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#readablestreambyobrequest
class ReadableStreamBYOBRequest : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(ReadableStreamBYOBRequest, Bindings::PlatformObject);

public:
    virtual ~ReadableStreamBYOBRequest() override = default;

    JS::GCPtr<JS::TypedArrayBase> view();

private:
    explicit ReadableStreamBYOBRequest(JS::Realm&);

    virtual void visit_edges(Cell::Visitor&) override;

    // https://streams.spec.whatwg.org/#readablestreambyobrequest-controller
    // The parent ReadableByteStreamController instance
    JS::GCPtr<JS::Object> m_controller;

    // https://streams.spec.whatwg.org/#readablestreambyobrequest-view
    // A typed array representing the destination region to which the controller can write generated data, or null after the BYOB request has been invalidated.
    JS::GCPtr<JS::TypedArrayBase> m_view;
};

}
