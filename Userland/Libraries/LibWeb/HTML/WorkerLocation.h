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
    static JS::NonnullGCPtr<WorkerLocation> create(WorkerGlobalScope&);

    virtual ~WorkerLocation() override;

    DeprecatedString href() const;
    DeprecatedString origin() const;
    DeprecatedString protocol() const;
    DeprecatedString host() const;
    DeprecatedString hostname() const;
    DeprecatedString port() const;
    DeprecatedString pathname() const;
    DeprecatedString search() const;
    DeprecatedString hash() const;

private:
    explicit WorkerLocation(WorkerGlobalScope&);

    virtual void visit_edges(Cell::Visitor&) override;

    WorkerGlobalScope& m_global_scope;
};

}
