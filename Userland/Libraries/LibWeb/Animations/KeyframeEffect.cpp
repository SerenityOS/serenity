/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/KeyframeEffect.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Animations {

JS_DEFINE_ALLOCATOR(KeyframeEffect);

JS::NonnullGCPtr<KeyframeEffect> KeyframeEffect::create(JS::Realm& realm)
{
    return realm.heap().allocate<KeyframeEffect>(realm, realm);
}

// https://www.w3.org/TR/web-animations-1/#dom-keyframeeffect-keyframeeffect
WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyframeEffect>> KeyframeEffect::construct_impl(
    JS::Realm& realm,
    JS::Handle<DOM::Element> const& target,
    Optional<JS::Handle<JS::Object>> const& keyframes,
    Variant<double, KeyframeEffectOptions> options)
{
    auto& vm = realm.vm();

    // 1. Create a new KeyframeEffect object, effect.
    auto effect = vm.heap().allocate<KeyframeEffect>(realm, realm);

    // 2. Set the target element of effect to target.
    effect->set_target(target);

    // 3. Set the target pseudo-selector to the result corresponding to the first matching condition from below.

    //    If options is a KeyframeEffectOptions object with a pseudoElement property,
    if (options.has<KeyframeEffectOptions>()) {
        // Set the target pseudo-selector to the value of the pseudoElement property.
        //
        // When assigning this property, the error-handling defined for the pseudoElement setter on the interface is
        // applied. If the setter requires an exception to be thrown, this procedure must throw the same exception and
        // abort all further steps.
        effect->set_pseudo_element(options.get<KeyframeEffectOptions>().pseudo_element);
    }
    //     Otherwise,
    else {
        // Set the target pseudo-selector to null.
        // Note: This is the default when constructed
    }

    // 4. Let timing input be the result corresponding to the first matching condition from below.
    KeyframeEffectOptions timing_input;

    //     If options is a KeyframeEffectOptions object,
    if (options.has<KeyframeEffectOptions>()) {
        // Let timing input be options.
        timing_input = options.get<KeyframeEffectOptions>();
    }
    //     Otherwise (if options is a double),
    else {
        // Let timing input be a new EffectTiming object with all members set to their default values and duration set
        // to options.
        timing_input.duration = options.get<double>();
    }

    // 5. Call the procedure to update the timing properties of an animation effect of effect from timing input.
    //    If that procedure causes an exception to be thrown, propagate the exception and abort this procedure.
    TRY(effect->update_timing(timing_input.to_optional_effect_timing()));

    // 6. If options is a KeyframeEffectOptions object, assign the composite property of effect to the corresponding
    //    value from options.
    //
    //    When assigning this property, the error-handling defined for the corresponding setter on the KeyframeEffect
    //    interface is applied. If the setter requires an exception to be thrown for the value specified by options,
    //    this procedure must throw the same exception and abort all further steps.
    if (options.has<KeyframeEffectOptions>())
        effect->set_composite(options.get<KeyframeEffectOptions>().composite);

    // 7. Initialize the set of keyframes by performing the procedure defined for setKeyframes() passing keyframes as
    //    the input.
    TRY(effect->set_keyframes(keyframes));

    return effect;
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyframeEffect>> KeyframeEffect::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<KeyframeEffect> source)
{
    auto& vm = realm.vm();

    // 1. Create a new KeyframeEffect object, effect.
    auto effect = vm.heap().allocate<KeyframeEffect>(realm, realm);

    // 2. Set the following properties of effect using the corresponding values of source:

    //   - effect target,
    effect->m_target_element = source->target();

    // FIXME:
    //   - keyframes,

    //   - composite operation, and
    effect->set_composite(source->composite());

    //   - all specified timing properties:

    //     - start delay,
    effect->m_start_delay = source->m_start_delay;

    //     - end delay,
    effect->m_end_delay = source->m_end_delay;

    //     - fill mode,
    effect->m_fill_mode = source->m_fill_mode;

    //     - iteration start,
    effect->m_iteration_start = source->m_iteration_start;

    //     - iteration count,
    effect->m_iteration_count = source->m_iteration_count;

    //     - iteration duration,
    effect->m_iteration_duration = source->m_iteration_duration;

    //     - playback direction, and
    effect->m_playback_direction = source->m_playback_direction;

    //     - timing function.
    effect->m_easing_function = source->m_easing_function;

    return effect;
}

void KeyframeEffect::set_pseudo_element(Optional<String> pseudo_element)
{
    // On setting, sets the target pseudo-selector of the animation effect to the provided value after applying the
    // following exceptions:

    // FIXME:
    // - If the provided value is not null and is an invalid <pseudo-element-selector>, the user agent must throw a
    //   DOMException with error name SyntaxError and leave the target pseudo-selector of this animation effect
    //   unchanged.

    // - If one of the legacy Selectors Level 2 single-colon selectors (':before', ':after', ':first-letter', or
    //   ':first-line') is specified, the target pseudo-selector must be set to the equivalent two-colon selector
    //   (e.g. '::before').
    if (pseudo_element.has_value()) {
        auto value = pseudo_element.value();

        if (value == ":before" || value == ":after" || value == ":first-letter" || value == ":first-line") {
            m_target_pseudo_selector = MUST(String::formatted(":{}", value));
            return;
        }
    }

    m_target_pseudo_selector = pseudo_element;
}

// https://www.w3.org/TR/web-animations-1/#dom-keyframeeffect-getkeyframes
WebIDL::ExceptionOr<Vector<JS::Object*>> KeyframeEffect::get_keyframes() const
{
    // FIXME: Implement this
    return Vector<JS::Object*> {};
}

// https://www.w3.org/TR/web-animations-1/#dom-keyframeeffect-setkeyframes
WebIDL::ExceptionOr<void> KeyframeEffect::set_keyframes(Optional<JS::Handle<JS::Object>> const&)
{
    // FIXME: Implement this
    return {};
}

KeyframeEffect::KeyframeEffect(JS::Realm& realm)
    : AnimationEffect(realm)
{
}

void KeyframeEffect::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::KeyframeEffectPrototype>(realm, "KeyframeEffect"_fly_string));
}

void KeyframeEffect::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_target_element);
}

}
