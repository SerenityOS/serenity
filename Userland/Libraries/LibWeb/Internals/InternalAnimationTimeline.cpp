/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Internals/InternalAnimationTimeline.h>

namespace Web::Internals {

JS_DEFINE_ALLOCATOR(InternalAnimationTimeline);

void InternalAnimationTimeline::set_current_time(Optional<double> current_time)
{
    // Do nothing
    (void)current_time;
}

void InternalAnimationTimeline::set_time(Optional<double> time)
{
    Base::set_current_time(time);
}

InternalAnimationTimeline::InternalAnimationTimeline(JS::Realm& realm)
    : AnimationTimeline(realm)
{
    m_current_time = 0.0;
}

void InternalAnimationTimeline::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(InternalAnimationTimeline);
}

}
