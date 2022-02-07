/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/workers.html#worker-locations
class WorkerLocation
    : public RefCounted<WorkerLocation>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::WorkerLocationWrapper;

    static NonnullRefPtr<WorkerLocation> create(WorkerGlobalScope& global_scope)
    {
        return adopt_ref(*new WorkerLocation(global_scope));
    }

    String href() const;
    String origin() const;
    String protocol() const;
    String host() const;
    String hostname() const;
    String port() const;
    String pathname() const;
    String search() const;
    String hash() const;

private:
    WorkerLocation(WorkerGlobalScope&);

    WorkerGlobalScope& m_global_scope;
};

}
