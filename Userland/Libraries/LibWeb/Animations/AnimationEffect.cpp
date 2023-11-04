/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Animations/Animation.h>
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

// https://www.w3.org/TR/web-animations-1/#animation-direction
AnimationDirection AnimationEffect::animation_direction() const
{
    // "backwards" if the effect is associated with an animation and the associated animation’s playback rate is less
    // than zero; in all other cases, the animation direction is "forwards".
    if (m_associated_animation && m_associated_animation->playback_rate() < 0.0)
        return AnimationDirection::Backwards;
    return AnimationDirection::Forwards;
}

// https://www.w3.org/TR/web-animations-1/#end-time
double AnimationEffect::end_time() const
{
    // 1. The end time of an animation effect is the result of evaluating
    //    max(start delay + active duration + end delay, 0).
    return max(m_start_delay + active_duration() + m_end_delay, 0.0);
}

// https://www.w3.org/TR/web-animations-1/#local-time
Optional<double> AnimationEffect::local_time() const
{
    // The local time of an animation effect at a given moment is based on the first matching condition from the
    // following:

    // -> If the animation effect is associated with an animation,
    if (m_associated_animation) {
        // the local time is the current time of the animation.
        return m_associated_animation->current_time();
    }

    // -> Otherwise,
    //    the local time is unresolved.
    return {};
}

// https://www.w3.org/TR/web-animations-1/#active-duration
double AnimationEffect::active_duration() const
{
    // The active duration is calculated as follows:
    //     active duration = iteration duration × iteration count
    // If either the iteration duration or iteration count are zero, the active duration is zero. This clarification is
    // needed since the result of infinity multiplied by zero is undefined according to IEEE 754-2008.
    if (m_iteration_duration.has<String>() || m_iteration_duration.get<double>() == 0.0 || m_iteration_count == 0.0)
        return 0.0;

    return m_iteration_duration.get<double>() * m_iteration_count;
}

// https://www.w3.org/TR/web-animations-1/#before-active-boundary-time
double AnimationEffect::before_active_boundary_time() const
{
    // max(min(start delay, end time), 0)
    return max(min(m_start_delay, end_time()), 0.0);
}

// https://www.w3.org/TR/web-animations-1/#active-after-boundary-time
double AnimationEffect::after_active_boundary_time() const
{
    // max(min(start delay + active duration, end time), 0)
    return max(min(m_start_delay + active_duration(), end_time()), 0.0);
}

// https://www.w3.org/TR/web-animations-1/#animation-effect-before-phase
bool AnimationEffect::is_in_the_before_phase() const
{
    // An animation effect is in the before phase if the animation effect’s local time is not unresolved and either of
    // the following conditions are met:
    auto local_time = this->local_time();
    if (!local_time.has_value())
        return false;

    // - the local time is less than the before-active boundary time, or
    auto before_active_boundary_time = this->before_active_boundary_time();
    if (local_time.value() < before_active_boundary_time)
        return true;

    // - the animation direction is "backwards" and the local time is equal to the before-active boundary time.
    return animation_direction() == AnimationDirection::Backwards && local_time.value() == before_active_boundary_time;
}

// https://www.w3.org/TR/web-animations-1/#animation-effect-after-phase
bool AnimationEffect::is_in_the_after_phase() const
{
    // An animation effect is in the after phase if the animation effect’s local time is not unresolved and either of
    // the following conditions are met:
    auto local_time = this->local_time();
    if (!local_time.has_value())
        return false;

    // - the local time is greater than the active-after boundary time, or
    auto after_active_boundary_time = this->after_active_boundary_time();
    if (local_time.value() > after_active_boundary_time)
        return true;

    // - the animation direction is "forwards" and the local time is equal to the active-after boundary time.
    return animation_direction() == AnimationDirection::Forwards && local_time.value() == after_active_boundary_time;
}

// https://www.w3.org/TR/web-animations-1/#animation-effect-active-phase
bool AnimationEffect::is_in_the_active_phase() const
{
    // An animation effect is in the active phase if the animation effect’s local time is not unresolved and it is not
    // in either the before phase nor the after phase.
    return local_time().has_value() && !is_in_the_before_phase() && !is_in_the_after_phase();
}

// https://www.w3.org/TR/web-animations-1/#animation-effect-idle-phase
bool AnimationEffect::is_in_the_idle_phase() const
{
    // It is often convenient to refer to the case when an animation effect is in none of the above phases as being in
    // the idle phase
    return !is_in_the_before_phase() && !is_in_the_active_phase() && !is_in_the_after_phase();
}

AnimationEffect::Phase AnimationEffect::phase() const
{
    // This is a convenience method that returns the phase of the animation effect, to avoid having to call all of the
    // phase functions separately.
    // FIXME: There is a lot of duplicated condition checking here which can probably be inlined into this function

    if (is_in_the_before_phase())
        return Phase::Before;
    if (is_in_the_active_phase())
        return Phase::Active;
    if (is_in_the_after_phase())
        return Phase::After;
    return Phase::Idle;
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
