/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Streams/AbstractOperations.h>

namespace Web::Streams {

class TransformStreamDefaultController : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(TransformStreamDefaultController, Bindings::PlatformObject);

public:
    explicit TransformStreamDefaultController(JS::Realm&);
    virtual ~TransformStreamDefaultController() override;

    Optional<double> desired_size();
    WebIDL::ExceptionOr<void> enqueue(Optional<JS::Value> chunk);
    WebIDL::ExceptionOr<void> error(Optional<JS::Value> reason = {});
    WebIDL::ExceptionOr<void> terminate();

    auto& flush_algorithm() { return m_flush_algorithm; }
    void set_flush_algorithm(Optional<FlushAlgorithm>&& value) { m_flush_algorithm = move(value); }

    auto& transform_algorithm() { return m_transform_algorithm; }
    void set_transform_algorithm(Optional<TransformAlgorithm>&& value) { m_transform_algorithm = move(value); }

    JS::GCPtr<TransformStream> stream() { return m_stream; }
    void set_stream(JS::GCPtr<TransformStream> stream) { m_stream = stream; }

private:
    virtual void initialize(JS::Realm&) override;

    virtual void visit_edges(Cell::Visitor&) override;

    // https://streams.spec.whatwg.org/#transformstreamdefaultcontroller-flushalgorithm
    Optional<FlushAlgorithm> m_flush_algorithm;

    // https://streams.spec.whatwg.org/#transformstreamdefaultcontroller-transformalgorithm
    Optional<TransformAlgorithm> m_transform_algorithm;

    // https://streams.spec.whatwg.org/#transformstreamdefaultcontroller-stream
    JS::GCPtr<TransformStream> m_stream;
};

}
