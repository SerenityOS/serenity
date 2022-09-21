/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

// FIXME: Add Mixin APIs from https://html.spec.whatwg.org/multipage/workers.html#the-workernavigator-object
class WorkerNavigator : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(WorkerNavigator, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<WorkerNavigator> create(WorkerGlobalScope&);

    virtual ~WorkerNavigator() override;

private:
    explicit WorkerNavigator(WorkerGlobalScope&);
};

}
