/*
 * Copyright (c) 2023-2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibWeb/Animations/KeyframeEffect.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Animations {

JS_DEFINE_ALLOCATOR(KeyframeEffect);

template<typename T>
WebIDL::ExceptionOr<Variant<T, Vector<T>>> convert_value_to_maybe_list(JS::Realm& realm, JS::Value value, Function<WebIDL::ExceptionOr<T>(JS::Value)>& value_converter)
{
    auto& vm = realm.vm();

    if (TRY(value.is_array(vm))) {
        Vector<T> offsets;

        auto iterator = TRY(JS::get_iterator(vm, value, JS::IteratorHint::Sync));
        auto values = TRY(JS::iterator_to_list(vm, iterator));
        for (auto const& element : values) {
            if (element.is_undefined()) {
                offsets.append({});
            } else {
                offsets.append(TRY(value_converter(element)));
            }
        }

        return offsets;
    }

    return TRY(value_converter(value));
}

enum AllowLists {
    Yes,
    No,
};

template<AllowLists AL>
using KeyframeType = Conditional<AL == AllowLists::Yes, BasePropertyIndexedKeyframe, BaseKeyframe>;

// https://www.w3.org/TR/web-animations-1/#process-a-keyframe-like-object
template<AllowLists AL>
static WebIDL::ExceptionOr<KeyframeType<AL>> process_a_keyframe_like_object(JS::Realm& realm, JS::GCPtr<JS::Object> keyframe_input)
{
    auto& vm = realm.vm();

    Function<WebIDL::ExceptionOr<Optional<double>>(JS::Value)> to_nullable_double = [&vm](JS::Value value) -> WebIDL::ExceptionOr<Optional<double>> {
        if (value.is_undefined())
            return Optional<double> {};
        return TRY(value.to_double(vm));
    };

    Function<WebIDL::ExceptionOr<String>(JS::Value)> to_string = [&vm](JS::Value value) -> WebIDL::ExceptionOr<String> {
        return TRY(value.to_string(vm));
    };

    Function<WebIDL::ExceptionOr<Bindings::CompositeOperationOrAuto>(JS::Value)> to_composite_operation = [&vm](JS::Value value) -> WebIDL::ExceptionOr<Bindings::CompositeOperationOrAuto> {
        if (value.is_undefined())
            return Bindings::CompositeOperationOrAuto::Auto;

        auto string_value = TRY(value.to_string(vm));
        if (string_value == "replace")
            return Bindings::CompositeOperationOrAuto::Replace;
        if (string_value == "add")
            return Bindings::CompositeOperationOrAuto::Add;
        if (string_value == "accumulate")
            return Bindings::CompositeOperationOrAuto::Accumulate;
        if (string_value == "auto")
            return Bindings::CompositeOperationOrAuto::Auto;

        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid composite value"sv };
    };

    // 1. Run the procedure to convert an ECMAScript value to a dictionary type with keyframe input as the ECMAScript
    //    value, and the dictionary type depending on the value of the allow lists flag as follows:
    //
    //    -> If allow lists is true, use the following dictionary type: <BasePropertyIndexedKeyframe>.
    //    -> Otherwise, use the following dictionary type: <BaseKeyframe>.
    //
    //    Store the result of this procedure as keyframe output.

    KeyframeType<AL> keyframe_output;
    auto offset = TRY(keyframe_input->get("offset"));
    auto easing = TRY(keyframe_input->get("easing"));
    if (easing.is_undefined())
        easing = JS::PrimitiveString::create(vm, "linear"_string);
    auto composite = TRY(keyframe_input->get("composite"));
    if (composite.is_undefined())
        composite = JS::PrimitiveString::create(vm, "auto"_string);

    if constexpr (AL == AllowLists::Yes) {
        keyframe_output.offset = TRY(convert_value_to_maybe_list(realm, offset, to_nullable_double));
        keyframe_output.composite = TRY(convert_value_to_maybe_list(realm, composite, to_composite_operation));

        auto easing_maybe_list = TRY(convert_value_to_maybe_list(realm, easing, to_string));
        easing_maybe_list.visit(
            [&](String const& value) {
                keyframe_output.easing = EasingValue { value };
            },
            [&](Vector<String> const& values) {
                Vector<EasingValue> easing_values;
                for (auto& easing_value : values)
                    easing_values.append(easing_value);
                keyframe_output.easing = move(easing_values);
            });
    } else {
        keyframe_output.offset = TRY(to_nullable_double(offset));
        keyframe_output.easing = TRY(to_string(easing));
        keyframe_output.composite = TRY(to_composite_operation(composite));
    }

    // 2. Build up a list of animatable properties as follows:
    //
    //    1. Let animatable properties be a list of property names (including shorthand properties that have longhand
    //       sub-properties that are animatable) that can be animated by the implementation.
    //    2. Convert each property name in animatable properties to the equivalent IDL attribute by applying the
    //       animation property name to IDL attribute name algorithm.

    // 3. Let input properties be the result of calling the EnumerableOwnNames operation with keyframe input as the
    //    object.

    // 4. Make up a new list animation properties that consists of all of the properties that are in both input
    //    properties and animatable properties, or which are in input properties and conform to the
    //    <custom-property-name> production.
    auto input_properties = TRY(keyframe_input->internal_own_property_keys());

    Vector<String> animation_properties;
    for (auto const& input_property : input_properties) {
        if (!input_property.is_string())
            continue;

        auto name = input_property.as_string().utf8_string();
        if (auto property = CSS::property_id_from_camel_case_string(name); property.has_value()) {
            if (CSS::is_animatable_property(property.value()))
                animation_properties.append(name);
        }
    }

    // 5. Sort animation properties in ascending order by the Unicode codepoints that define each property name.
    quick_sort(animation_properties);

    // 6. For each property name in animation properties,
    for (auto const& property_name : animation_properties) {
        // 1. Let raw value be the result of calling the [[Get]] internal method on keyframe input, with property name
        //    as the property key and keyframe input as the receiver.
        // 2. Check the completion record of raw value.
        auto raw_value = TRY(keyframe_input->get(ByteString { property_name }));

        using PropertyValuesType = Conditional<AL == AllowLists::Yes, Vector<String>, String>;
        PropertyValuesType property_values;

        // 3. Convert raw value to a DOMString or sequence of DOMStrings property values as follows:

        // -> If allow lists is true,
        if constexpr (AL == AllowLists::Yes) {
            // Let property values be the result of converting raw value to IDL type (DOMString or sequence<DOMString>)
            // using the procedures defined for converting an ECMAScript value to an IDL value [WEBIDL].
            auto intermediate_property_values = TRY(convert_value_to_maybe_list(realm, raw_value, to_string));

            // If property values is a single DOMString, replace property values with a sequence of DOMStrings with the
            // original value of property values as the only element.
            if (intermediate_property_values.has<String>())
                property_values = Vector { intermediate_property_values.get<String>() };
            else
                property_values = intermediate_property_values.get<Vector<String>>();
        }
        // -> Otherwise,
        else {
            // Let property values be the result of converting raw value to a DOMString using the procedure for
            // converting an ECMAScript value to a DOMString [WEBIDL].
            property_values = TRY(raw_value.to_string(vm));
        }

        // 4. Calculate the normalized property name as the result of applying the IDL attribute name to animation
        //    property name algorithm to property name.
        // Note: We do not need to do this, since we did not need to do the reverse step (animation property name to IDL
        //       attribute name) in the steps above.

        // 5. Add a property to keyframe output with normalized property name as the property name, and property values
        //    as the property value.
        if constexpr (AL == AllowLists::Yes) {
            keyframe_output.properties.set(property_name, property_values);
        } else {
            keyframe_output.unparsed_properties().set(property_name, property_values);
        }
    }

    return keyframe_output;
}

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
