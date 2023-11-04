/*
 * Copyright (c) 2023-2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/AnimationPrototype.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#the-animation-interface
class Animation : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Animation, DOM::EventTarget);
    JS_DECLARE_ALLOCATOR(Animation);

public:
    static JS::NonnullGCPtr<Animation> create(JS::Realm&, JS::GCPtr<AnimationEffect>, JS::GCPtr<AnimationTimeline>);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Animation>> construct_impl(JS::Realm&, JS::GCPtr<AnimationEffect>, JS::GCPtr<AnimationTimeline>);

    FlyString const& id() const { return m_id; }
    void set_id(FlyString value) { m_id = move(value); }

    JS::GCPtr<AnimationEffect> effect() const { return m_effect; }
    void set_effect(JS::GCPtr<AnimationEffect>);

    JS::GCPtr<AnimationTimeline> timeline() const { return m_timeline; }
    void set_timeline(JS::GCPtr<AnimationTimeline>);

    Optional<double> const& start_time() const { return m_start_time; }
    void set_start_time(Optional<double> const&);

    Optional<double> current_time() const;
    WebIDL::ExceptionOr<void> set_current_time(Optional<double> const&);

    double playback_rate() const { return m_playback_rate; }
    WebIDL::ExceptionOr<void> set_playback_rate(double value);

    Bindings::AnimationPlayState play_state() const;

    Bindings::AnimationReplaceState replace_state() const { return m_replace_state; }

    // https://www.w3.org/TR/web-animations-1/#dom-animation-pending
    bool pending() const { return m_pending_play_task == TaskState::Scheduled || m_pending_pause_task == TaskState::Scheduled; }

    // https://www.w3.org/TR/web-animations-1/#dom-animation-ready
    JS::NonnullGCPtr<JS::Object> ready() const { return *current_ready_promise()->promise(); }

    // https://www.w3.org/TR/web-animations-1/#dom-animation-finished
    JS::NonnullGCPtr<JS::Object> finished() const { return *current_finished_promise()->promise(); }

    enum class AutoRewind {
        Yes,
        No,
    };
    WebIDL::ExceptionOr<void> play();
    WebIDL::ExceptionOr<void> play_an_animation(AutoRewind);

    Optional<double> convert_an_animation_time_to_timeline_time(Optional<double>) const;
    Optional<double> convert_a_timeline_time_to_an_origin_relative_time(Optional<double>) const;

    JS::GCPtr<DOM::Document> document_for_timing() const;
    void notify_timeline_time_did_change();

    void effect_timing_changed(Badge<AnimationEffect>);

    virtual bool is_css_animation() const { return false; }

protected:
    Animation(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

private:
    enum class TaskState {
        None,
        Scheduled,
    };

    enum class DidSeek {
        Yes,
        No,
    };

    enum class SynchronouslyNotify {
        Yes,
        No,
    };

    double associated_effect_end() const;
    double effective_playback_rate() const;

    void apply_any_pending_playback_rate();
    WebIDL::ExceptionOr<void> silently_set_current_time(Optional<double>);
    void update_finished_state(DidSeek, SynchronouslyNotify);

    void run_pending_play_task();
    void run_pending_pause_task();

    JS::NonnullGCPtr<WebIDL::Promise> current_ready_promise() const;
    JS::NonnullGCPtr<WebIDL::Promise> current_finished_promise() const;

    // https://www.w3.org/TR/web-animations-1/#dom-animation-id
    FlyString m_id;

    // https://www.w3.org/TR/web-animations-1/#dom-animation-effect
    JS::GCPtr<AnimationEffect> m_effect;

    // https://www.w3.org/TR/web-animations-1/#dom-animation-timeline
    JS::GCPtr<AnimationTimeline> m_timeline;

    // https://www.w3.org/TR/web-animations-1/#animation-start-time
    Optional<double> m_start_time {};

    // https://www.w3.org/TR/web-animations-1/#animation-hold-time
    Optional<double> m_hold_time {};

    // https://www.w3.org/TR/web-animations-1/#previous-current-time
    Optional<double> m_previous_current_time {};

    // https://www.w3.org/TR/web-animations-1/#playback-rate
    double m_playback_rate { 1.0 };

    // https://www.w3.org/TR/web-animations-1/#pending-playback-rate
    Optional<double> m_pending_playback_rate {};

    // https://www.w3.org/TR/web-animations-1/#dom-animation-replacestate
    Bindings::AnimationReplaceState m_replace_state { Bindings::AnimationReplaceState::Active };

    // Note: The following promises are initialized lazily to avoid constructing them outside of an execution context
    // https://www.w3.org/TR/web-animations-1/#current-ready-promise
    mutable JS::GCPtr<WebIDL::Promise> m_current_ready_promise;

    // https://www.w3.org/TR/web-animations-1/#current-finished-promise
    mutable JS::GCPtr<WebIDL::Promise> m_current_finished_promise;
    bool m_current_finished_promise_resolved { false };

    // https://www.w3.org/TR/web-animations-1/#pending-play-task
    TaskState m_pending_play_task { TaskState::None };

    // https://www.w3.org/TR/web-animations-1/#pending-pause-task
    TaskState m_pending_pause_task { TaskState::None };

    // Flags used to manage the finish notification microtask and ultimately prevent more than one finish notification
    // microtask from being queued at any given time
    bool m_should_abort_finish_notification_microtask { false };
    bool m_has_finish_notification_microtask_scheduled { false };
};

}
