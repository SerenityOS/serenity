/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Streams/TransformStream.h>
#include <LibWeb/Streams/TransformStreamDefaultController.h>

namespace Web::Streams {

TransformStreamDefaultController::TransformStreamDefaultController(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

TransformStreamDefaultController::~TransformStreamDefaultController() = default;

void TransformStreamDefaultController::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TransformStreamDefaultControllerPrototype>(realm, "TransformStreamDefaultController"));
}

void TransformStreamDefaultController::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_stream);
}

// https://streams.spec.whatwg.org/#ts-default-controller-desired-size
Optional<double> TransformStreamDefaultController::desired_size()
{
    VERIFY(stream()->readable()->controller().has_value() && stream()->readable()->controller()->has<JS::NonnullGCPtr<ReadableStreamDefaultController>>());

    // 1. Let readableController be this.[[stream]].[[readable]].[[controller]].
    auto readable_controller = stream()->readable()->controller()->get<JS::NonnullGCPtr<ReadableStreamDefaultController>>();

    // 2. Return ! ReadableStreamDefaultControllerGetDesiredSize(readableController).
    return readable_stream_default_controller_get_desired_size(*readable_controller);
}

// https://streams.spec.whatwg.org/#ts-default-controller-enqueue
WebIDL::ExceptionOr<void> TransformStreamDefaultController::enqueue(Optional<JS::Value> chunk)
{
    // 1. Perform ? TransformStreamDefaultControllerEnqueue(this, chunk).
    TRY(transform_stream_default_controller_enqueue(*this, chunk.has_value() ? chunk.value() : JS::js_undefined()));

    return {};
}

// https://streams.spec.whatwg.org/#ts-default-controller-error
WebIDL::ExceptionOr<void> TransformStreamDefaultController::error(Optional<JS::Value> reason)
{
    // 1. Perform ? TransformStreamDefaultControllerError(this, e).
    TRY(transform_stream_default_controller_error(*this, reason.has_value() ? reason.value() : JS::js_undefined()));

    return {};
}

// https://streams.spec.whatwg.org/#ts-default-controller-terminate
WebIDL::ExceptionOr<void> TransformStreamDefaultController::terminate()
{
    // 1. Perform ? TransformStreamDefaultControllerTerminate(this).
    TRY(transform_stream_default_controller_terminate(*this));

    return {};
}

}
