/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/Animations/KeyframeEffect.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#dictdef-keyframeanimationoptions
struct KeyframeAnimationOptions : public KeyframeEffectOptions {
    FlyString id { ""_fly_string };
    JS::GCPtr<AnimationTimeline> timeline;
};

// https://www.w3.org/TR/web-animations-1/#dictdef-getanimationsoptions
struct GetAnimationsOptions {
    bool subtree { false };
};

// https://www.w3.org/TR/web-animations-1/#animatable
class Animatable {
public:
    virtual ~Animatable() = default;

    WebIDL::ExceptionOr<JS::NonnullGCPtr<Animation>> animate(Optional<JS::Handle<JS::Object>> keyframes, Variant<Empty, double, KeyframeAnimationOptions> options = {});
    Vector<JS::NonnullGCPtr<Animation>> get_animations(GetAnimationsOptions options = {});

    void associate_with_effect(JS::NonnullGCPtr<AnimationEffect> effect);
    void disassociate_with_effect(JS::NonnullGCPtr<AnimationEffect> effect);

private:
    Vector<JS::NonnullGCPtr<AnimationEffect>> m_associated_effects;
    bool m_is_sorted_by_composite_order { true };
};

}
