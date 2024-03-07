/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibWeb/Animations/Animatable.h>
#include <LibWeb/Animations/Animation.h>
#include <LibWeb/Animations/DocumentTimeline.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#dom-animatable-animate
WebIDL::ExceptionOr<JS::NonnullGCPtr<Animation>> Animatable::animate(Optional<JS::Handle<JS::Object>> keyframes, Variant<Empty, double, KeyframeAnimationOptions> options)
{
    // 1. Let target be the object on which this method was called.
    JS::NonnullGCPtr target { *static_cast<DOM::Element*>(this) };
    auto& realm = target->realm();

    // 2. Construct a new KeyframeEffect object, effect, in the relevant Realm of target by using the same procedure as
    //    the KeyframeEffect(target, keyframes, options) constructor, passing target as the target argument, and the
    //    keyframes and options arguments as supplied.
    //
    //    If the above procedure causes an exception to be thrown, propagate the exception and abort this procedure.
    auto effect = TRY(options.visit(
        [&](Empty) { return KeyframeEffect::construct_impl(realm, target, keyframes); },
        [&](auto const& value) { return KeyframeEffect::construct_impl(realm, target, keyframes, value); }));

    // 3. If options is a KeyframeAnimationOptions object, let timeline be the timeline member of options or, if
    //    timeline member of options is missing, be the default document timeline of the node document of the element
    //    on which this method was called.
    JS::GCPtr<AnimationTimeline> timeline;
    if (options.has<KeyframeAnimationOptions>())
        timeline = options.get<KeyframeAnimationOptions>().timeline;
    if (!timeline)
        timeline = target->document().timeline();

    // 4. Construct a new Animation object, animation, in the relevant Realm of target by using the same procedure as
    //    the Animation() constructor, passing effect and timeline as arguments of the same name.
    auto animation = TRY(Animation::construct_impl(realm, effect, timeline));

    // 5. If options is a KeyframeAnimationOptions object, assign the value of the id member of options to animation’s
    //    id attribute.
    if (options.has<KeyframeAnimationOptions>())
        animation->set_id(options.get<KeyframeAnimationOptions>().id);

    //  6. Run the procedure to play an animation for animation with the auto-rewind flag set to true.
    TRY(animation->play_an_animation(Animation::AutoRewind::Yes));

    // 7. Return animation.
    return animation;
}

// https://www.w3.org/TR/web-animations-1/#dom-animatable-getanimations
Vector<JS::NonnullGCPtr<Animation>> Animatable::get_animations(Web::Animations::GetAnimationsOptions options)
{
    // Returns the set of relevant animations for this object, or, if an options parameter is passed with subtree set to
    // true, returns the set of relevant animations for a subtree for this object.

    // The returned list is sorted using the composite order described for the associated animations of effects in
    // §5.4.2 The effect stack.
    if (!m_is_sorted_by_composite_order) {
        quick_sort(m_associated_effects, [](JS::NonnullGCPtr<AnimationEffect>& a, JS::NonnullGCPtr<AnimationEffect>& b) {
            auto& a_effect = verify_cast<KeyframeEffect>(*a);
            auto& b_effect = verify_cast<KeyframeEffect>(*b);
            return KeyframeEffect::composite_order(a_effect, b_effect) < 0;
        });
        m_is_sorted_by_composite_order = true;
    }

    // FIXME: Support subtree
    (void)options;

    Vector<JS::NonnullGCPtr<Animation>> relevant_animations;
    for (auto& effect : m_associated_effects) {
        if (auto animation = effect->associated_animation(); animation && animation->is_relevant())
            relevant_animations.append(*animation);
    }

    return relevant_animations;
}

void Animatable::associate_with_effect(JS::NonnullGCPtr<AnimationEffect> effect)
{
    m_associated_effects.append(effect);
    m_is_sorted_by_composite_order = false;
}

void Animatable::disassociate_with_effect(JS::NonnullGCPtr<AnimationEffect> effect)
{
    m_associated_effects.remove_first_matching([&](auto element) { return effect == element; });
}

void Animatable::visit_edges(JS::Cell::Visitor& visitor)
{
    for (auto const& effect : m_associated_effects)
        visitor.visit(effect);
    visitor.visit(m_cached_animation_name_source);
    visitor.visit(m_cached_animation_name_animation);
}

}
