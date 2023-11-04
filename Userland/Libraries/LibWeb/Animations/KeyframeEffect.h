/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibWeb/Animations/AnimationEffect.h>
#include <LibWeb/Bindings/KeyframeEffectPrototype.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Animations {

// https://www.w3.org/TR/web-animations-1/#the-keyframeeffectoptions-dictionary
struct KeyframeEffectOptions : public EffectTiming {
    Bindings::CompositeOperation composite { Bindings::CompositeOperation::Replace };
    Optional<String> pseudo_element {};
};

// https://www.w3.org/TR/web-animations-1/#dictdef-basepropertyindexedkeyframe
struct BasePropertyIndexedKeyframe {
    Variant<Optional<double>, Vector<Optional<double>>> offset { Vector<Optional<double>> {} };
    Variant<String, Vector<String>> easing { Vector<String> {} };
    Variant<Bindings::CompositeOperationOrAuto, Vector<Bindings::CompositeOperationOrAuto>> composite { Vector<Bindings::CompositeOperationOrAuto> {} };
};

// https://www.w3.org/TR/web-animations-1/#dictdef-basekeyframe
struct BaseKeyframe {
    Optional<double> offset {};
    String easing { "linear"_string };
    Bindings::CompositeOperationOrAuto composite { Bindings::CompositeOperationOrAuto::Auto };

    Optional<double> computed_offset {};
};

// https://www.w3.org/TR/web-animations-1/#the-keyframeeffect-interface
class KeyframeEffect : public AnimationEffect {
    WEB_PLATFORM_OBJECT(KeyframeEffect, AnimationEffect);

public:
    static JS::NonnullGCPtr<KeyframeEffect> create(JS::Realm&);

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyframeEffect>> construct_impl(
        JS::Realm&,
        JS::Handle<DOM::Element> const& target,
        JS::Handle<JS::Object> const& keyframes,
        Variant<double, KeyframeEffectOptions> options = KeyframeEffectOptions {});

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<KeyframeEffect>> construct_impl(JS::Realm&, JS::NonnullGCPtr<KeyframeEffect> source);

    DOM::Element* target() const override { return m_target_element; }
    void set_target(DOM::Element* target) { m_target_element = target; }

    Optional<String> pseudo_element() const { return m_target_pseudo_selector; }
    void set_pseudo_element(Optional<String>);

    Bindings::CompositeOperation composite() const { return m_composite; }
    void set_composite(Bindings::CompositeOperation value) { m_composite = value; }

    WebIDL::ExceptionOr<Vector<JS::Object*>> get_keyframes() const;
    WebIDL::ExceptionOr<void> set_keyframes(JS::Object*);

private:
    KeyframeEffect(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // https://www.w3.org/TR/web-animations-1/#effect-target-target-element
    JS::GCPtr<DOM::Element> m_target_element {};

    // https://www.w3.org/TR/web-animations-1/#dom-keyframeeffect-pseudoelement
    Optional<String> m_target_pseudo_selector {};

    // https://www.w3.org/TR/web-animations-1/#dom-keyframeeffect-composite
    Bindings::CompositeOperation m_composite { Bindings::CompositeOperation::Replace };

    // https://www.w3.org/TR/web-animations-1/#keyframe
    Vector<BaseKeyframe> m_keyframes {};
};

}
