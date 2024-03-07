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

    void associate_with_animation(JS::NonnullGCPtr<Animation>);
    void disassociate_with_animation(JS::NonnullGCPtr<Animation>);

    JS::GCPtr<CSS::CSSStyleDeclaration const> cached_animation_name_source() const { return m_cached_animation_name_source; }
    void set_cached_animation_name_source(JS::GCPtr<CSS::CSSStyleDeclaration const> value) { m_cached_animation_name_source = value; }

    JS::GCPtr<Animations::Animation> cached_animation_name_animation() const { return m_cached_animation_name_animation; }
    void set_cached_animation_name_animation(JS::GCPtr<Animations::Animation> value) { m_cached_animation_name_animation = value; }

protected:
    void visit_edges(JS::Cell::Visitor&);

private:
    Vector<JS::NonnullGCPtr<Animation>> m_associated_animations;
    bool m_is_sorted_by_composite_order { true };
    JS::GCPtr<CSS::CSSStyleDeclaration const> m_cached_animation_name_source;
    JS::GCPtr<Animations::Animation> m_cached_animation_name_animation;
};

}
