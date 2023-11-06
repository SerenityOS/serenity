/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Animations/AnimationEffect.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Animations {

JS::NonnullGCPtr<AnimationEffect> AnimationEffect::create(JS::Realm& realm)
{
    return realm.heap().allocate<AnimationEffect>(realm, realm);
}

OptionalEffectTiming EffectTiming::to_optional_effect_timing() const
{
    return {
        .delay = delay,
        .end_delay = end_delay,
        .fill = fill,
        .iteration_start = iteration_start,
        .iterations = iterations,
        .duration = duration,
        .direction = direction,
        .easing = easing,
    };
}

// https://www.w3.org/TR/web-animations-1/#dom-animationeffect-gettiming
EffectTiming AnimationEffect::get_timing() const
{
    // 1. Returns the specified timing properties for this animation effect.
    return {
        .delay = m_start_delay,
        .end_delay = m_end_delay,
        .fill = m_fill_mode,
        .iteration_start = m_iteration_start,
        .iterations = m_iteration_count,
        .duration = m_iteration_duration,
        .direction = m_playback_direction,
        .easing = m_easing_function,
    };
}

// https://www.w3.org/TR/web-animations-1/#dom-animationeffect-getcomputedtiming
ComputedEffectTiming AnimationEffect::get_computed_timing() const
{
    // 1. Returns the calculated timing properties for this animation effect.

    // Note: Although some of the attributes of the object returned by getTiming() and getComputedTiming() are common,
    //       their values may differ in the following ways:

    //     - duration: while getTiming() may return the string auto, getComputedTiming() must return a number
    //       corresponding to the calculated value of the iteration duration as defined in the description of the
    //       duration member of the EffectTiming interface.
    //
    //       In this level of the specification, that simply means that an auto value is replaced by zero.
    auto duration = m_iteration_duration.has<String>() ? 0.0 : m_iteration_duration.get<double>();

    //     - fill: likewise, while getTiming() may return the string auto, getComputedTiming() must return the specific
    //       FillMode used for timing calculations as defined in the description of the fill member of the EffectTiming
    //       interface.
    //
    //       In this level of the specification, that simply means that an auto value is replaced by the none FillMode.
    auto fill = m_fill_mode == Bindings::FillMode::Auto ? Bindings::FillMode::None : m_fill_mode;

    return {
        {
            .delay = m_start_delay,
            .end_delay = m_end_delay,
            .fill = fill,
            .iteration_start = m_iteration_start,
            .iterations = m_iteration_count,
            .duration = duration,
            .direction = m_playback_direction,
            .easing = m_easing_function,
        },

        // FIXME:
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
    };
}

// https://www.w3.org/TR/web-animations-1/#dom-animationeffect-updatetiming
// https://www.w3.org/TR/web-animations-1/#update-the-timing-properties-of-an-animation-effect
WebIDL::ExceptionOr<void> AnimationEffect::update_timing(OptionalEffectTiming timing)
{
    // 1. If the iterationStart member of input exists and is less than zero, throw a TypeError and abort this
    //    procedure.
    if (timing.iteration_start.has_value() && timing.iteration_start.value() < 0.0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid iteration start value"sv };

    // 2. If the iterations member of input exists, and is less than zero or is the value NaN, throw a TypeError and
    //    abort this procedure.
    if (timing.iterations.has_value() && (timing.iterations.value() < 0.0 || isnan(timing.iterations.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid iteration count value"sv };

    // 3. If the duration member of input exists, and is less than zero or is the value NaN, throw a TypeError and
    //    abort this procedure.
    // Note: "auto", the only valid string value, is treated as 0.
    auto& duration = timing.duration;
    if (duration.has_value() && duration->has<double>() && (duration->get<double>() < 0.0 || isnan(duration->get<double>())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid duration value"sv };

    // FIXME:
    // 4. If the easing member of input exists but cannot be parsed using the <easing-function> production
    //    [CSS-EASING-1], throw a TypeError and abort this procedure.

    // 5. Assign each member that exists in input to the corresponding timing property of effect as follows:

    //    - delay → start delay
    if (timing.delay.has_value())
        m_start_delay = timing.delay.value();

    //    - endDelay → end delay
    if (timing.end_delay.has_value())
        m_end_delay = timing.end_delay.value();

    //    - fill → fill mode
    if (timing.fill.has_value())
        m_fill_mode = timing.fill.value();

    //    - iterationStart → iteration start
    if (timing.iteration_start.has_value())
        m_iteration_start = timing.iteration_start.value();

    //    - iterations → iteration count
    if (timing.iterations.has_value())
        m_iteration_count = timing.iterations.value();

    //    - duration → iteration duration
    if (timing.duration.has_value())
        m_iteration_duration = timing.duration.value();

    //    - direction → playback direction
    if (timing.direction.has_value())
        m_playback_direction = timing.direction.value();

    //    - easing → timing function
    if (timing.easing.has_value())
        m_easing_function = timing.easing.value();

    return {};
}

AnimationEffect::AnimationEffect(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

void AnimationEffect::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AnimationEffectPrototype>(realm, "AnimationEffect"));
}

}
