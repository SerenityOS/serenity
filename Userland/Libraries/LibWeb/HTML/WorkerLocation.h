/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/workers.html#worker-locations
class WorkerLocation : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(WorkerLocation, Bindings::PlatformObject);

public:
    virtual ~WorkerLocation() override;

    WebIDL::ExceptionOr<String> href() const;
    WebIDL::ExceptionOr<String> origin() const;
    WebIDL::ExceptionOr<String> protocol() const;
    WebIDL::ExceptionOr<String> host() const;
    WebIDL::ExceptionOr<String> hostname() const;
    WebIDL::ExceptionOr<String> port() const;
    WebIDL::ExceptionOr<String> pathname() const;
    WebIDL::ExceptionOr<String> search() const;
    WebIDL::ExceptionOr<String> hash() const;

private:
    explicit WorkerLocation(WorkerGlobalScope&);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<WorkerGlobalScope> m_global_scope;
};

}
