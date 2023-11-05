/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/Animatable.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#dom-animatable-animate
WebIDL::ExceptionOr<JS::NonnullGCPtr<Animation>> Animatable::animate(Optional<JS::Handle<JS::Object>> keyframes, Variant<Empty, double, KeyframeAnimationOptions> options)
{
    // FIXME: Implement this
    (void)keyframes;
    (void)options;
    return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Element.animate is not implemented"sv };
}

// https://www.w3.org/TR/web-animations-1/#dom-animatable-getanimations
Vector<JS::NonnullGCPtr<Animation>> Animatable::get_animations(Web::Animations::GetAnimationsOptions options)
{
    // FIXME: Implement this
    (void)options;
    return {};
}

}
