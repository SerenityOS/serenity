/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/Animation.h>
#include <LibWeb/Animations/AnimationEffect.h>
#include <LibWeb/Animations/DocumentTimeline.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#dom-animation-animation
JS::NonnullGCPtr<Animation> Animation::create(JS::Realm& realm, JS::GCPtr<AnimationEffect> effect, JS::GCPtr<AnimationTimeline> timeline)
{
    // 1. Let animation be a new Animation object.
    auto animation = realm.heap().allocate<Animation>(realm, realm);

    // 2. Run the procedure to set the timeline of an animation on animation passing timeline as the new timeline or, if
    //    a timeline argument is missing, passing the default document timeline of the Document associated with the
    //    Window that is the current global object.
    if (!timeline) {
        auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
        timeline = window.associated_document().timeline();
    }
    animation->set_timeline(timeline);

    // 3. Run the procedure to set the associated effect of an animation on animation passing source as the new effect.
    animation->set_effect(effect);

    return animation;
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<Animation>> Animation::construct_impl(JS::Realm& realm, JS::GCPtr<AnimationEffect> effect, JS::GCPtr<AnimationTimeline> timeline)
{
    return create(realm, effect, timeline);
}

// https://www.w3.org/TR/web-animations-1/#animation-set-the-associated-effect-of-an-animation
void Animation::set_effect(JS::GCPtr<AnimationEffect> new_effect)
{
    // Setting this attribute updates the object’s associated effect using the procedure to set the associated effect of
    // an animation.

    // 1. Let old effect be the current associated effect of animation, if any.
    auto old_effect = m_effect;

    // 2. If new effect is the same object as old effect, abort this procedure.
    if (new_effect == old_effect)
        return;

    // FIXME: 3. If animation has a pending pause task, reschedule that task to run as soon as animation is ready.

    // FIXME: 4. If animation has a pending play task, reschedule that task to run as soon as animation is ready to play
    //           new effect.

    // 5. If new effect is not null and if new effect is the associated effect of another animation, previous animation,
    //    run the procedure to set the associated effect of an animation (this procedure) on previous animation passing
    //    null as new effect.
    if (new_effect && new_effect->associated_animation() != this) {
        if (auto animation = new_effect->associated_animation())
            animation->set_effect({});
    }

    // 6. Let the associated effect of animation be new effect.
    if (new_effect)
        new_effect->set_associated_animation(this);
    if (m_effect)
        m_effect->set_associated_animation({});
    m_effect = new_effect;

    // FIXME: 7. Run the procedure to update an animation’s finished state for animation with the did seek flag set to
    //           false, and the synchronously notify flag set to false.
}

// https://www.w3.org/TR/web-animations-1/#animation-set-the-timeline-of-an-animation
void Animation::set_timeline(JS::GCPtr<AnimationTimeline> new_timeline)
{
    // Setting this attribute updates the object’s timeline using the procedure to set the timeline of an animation.

    // 1. Let old timeline be the current timeline of animation, if any.
    auto old_timeline = m_timeline;

    // 2. If new timeline is the same object as old timeline, abort this procedure.
    if (new_timeline == old_timeline)
        return;

    // 3. Let the timeline of animation be new timeline.
    if (m_timeline)
        m_timeline->disassociate_with_animation(*this);
    m_timeline = new_timeline;
    m_timeline->associate_with_animation(*this);

    // 4. If the start time of animation is resolved, make animation’s hold time unresolved.
    if (m_start_time.has_value())
        m_hold_time = {};

    // FIXME: 5. Run the procedure to update an animation’s finished state for animation with the did seek flag set to
    //           false, and the synchronously notify flag set to false.
}

// https://www.w3.org/TR/web-animations-1/#dom-animation-starttime
// https://www.w3.org/TR/web-animations-1/#set-the-start-time
void Animation::set_start_time(Optional<double> const& new_start_time)
{
    // Setting this attribute updates the start time using the procedure to set the start time of this object to the new
    // value.

    // 1. Let timeline time be the current time value of the timeline that animation is associated with. If there is no
    //    timeline associated with animation or the associated timeline is inactive, let the timeline time be
    //    unresolved.
    auto timeline_time = m_timeline && !m_timeline->is_inactive() ? m_timeline->current_time() : Optional<double> {};

    // 2. If timeline time is unresolved and new start time is resolved, make animation’s hold time unresolved.
    if (!timeline_time.has_value() && new_start_time.has_value())
        m_hold_time = {};

    // 3. Let previous current time be animation’s current time.
    auto previous_current_time = current_time();

    // 4. Apply any pending playback rate on animation.
    apply_any_pending_playback_rate();

    // 5. Set animation’s start time to new start time.
    m_start_time = new_start_time;

    // 6. Update animation’s hold time based on the first matching condition from the following,

    // -> If new start time is resolved,
    if (new_start_time.has_value()) {
        // If animation’s playback rate is not zero, make animation’s hold time unresolved.
        if (m_playback_rate != 0.0)
            m_hold_time = {};
    }
    // -> Otherwise (new start time is unresolved),
    else {
        // Set animation’s hold time to previous current time even if previous current time is unresolved.
        m_hold_time = previous_current_time;
    }

    // FIXME: 7. If animation has a pending play task or a pending pause task, cancel that task and resolve animation’s
    //           current ready promise with animation.

    // FIXME: 8. Run the procedure to update an animation’s finished state for animation with the did seek flag set to
    //           true, and the synchronously notify flag set to false.
}

// https://www.w3.org/TR/web-animations-1/#animation-current-time
Optional<double> Animation::current_time() const
{
    // FIXME: Implement
    return {};
}

// https://www.w3.org/TR/web-animations-1/#animation-set-the-current-time
WebIDL::ExceptionOr<void> Animation::set_current_time(Optional<double> const& seek_time)
{
    // FIXME: Implement
    (void)seek_time;
    return {};
}

// https://www.w3.org/TR/web-animations-1/#dom-animation-playbackrate
// https://www.w3.org/TR/web-animations-1/#set-the-playback-rate
WebIDL::ExceptionOr<void> Animation::set_playback_rate(double new_playback_rate)
{
    // FIXME: Implement
    (void)new_playback_rate;
    return {};
}

// https://www.w3.org/TR/web-animations-1/#animation-play-state
Bindings::AnimationPlayState Animation::play_state() const
{
    // FIXME: Implement
    return Bindings::AnimationPlayState::Idle;
}

// https://www.w3.org/TR/web-animations-1/#apply-any-pending-playback-rate
void Animation::apply_any_pending_playback_rate()
{
    // 1. If animation does not have a pending playback rate, abort these steps.
    if (!m_pending_playback_rate.has_value())
        return;

    // 2. Set animation’s playback rate to its pending playback rate.
    m_playback_rate = m_pending_playback_rate.value();

    // 3. Clear animation’s pending playback rate.
    m_pending_playback_rate = {};
}

JS::NonnullGCPtr<WebIDL::Promise> Animation::current_ready_promise() const
{
    if (!m_current_ready_promise) {
        // The current ready promise is initially a resolved Promise created using the procedure to create a new
        // resolved Promise with the animation itself as its value and created in the relevant Realm of the animation.
        m_current_ready_promise = WebIDL::create_resolved_promise(realm(), this);
    }

    return *m_current_ready_promise;
}

JS::NonnullGCPtr<WebIDL::Promise> Animation::current_finished_promise() const
{
    if (!m_current_finished_promise) {
        // The current finished promise is initially a pending Promise object.
        m_current_finished_promise = WebIDL::create_promise(realm());
    }

    return *m_current_finished_promise;
}

Animation::Animation(JS::Realm& realm)
    : DOM::EventTarget(realm)
{
}

void Animation::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AnimationPrototype>(realm, "Animation"));
}

void Animation::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_effect);
    visitor.visit(m_timeline);
    visitor.visit(m_current_ready_promise);
    visitor.visit(m_current_finished_promise);
}

}
