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

// FIXME: Add Mixin APIs from https://html.spec.whatwg.org/multipage/workers.html#the-workernavigator-object
class WorkerNavigator
    : public RefCounted<WorkerNavigator>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::WorkerNavigatorWrapper;
};

}
