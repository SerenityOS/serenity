/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <LibWeb/Bindings/CustomElementRegistryPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/CustomElements/CustomElementName.h>
#include <LibWeb/HTML/CustomElements/CustomElementReactionNames.h>
#include <LibWeb/HTML/CustomElements/CustomElementRegistry.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::HTML {

CustomElementRegistry::CustomElementRegistry(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

CustomElementRegistry::~CustomElementRegistry() = default;

void CustomElementRegistry::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CustomElementRegistryPrototype>(realm, "CustomElementRegistry"));
}

// https://webidl.spec.whatwg.org/#es-callback-function
static JS::ThrowCompletionOr<JS::NonnullGCPtr<WebIDL::CallbackType>> convert_value_to_callback_function(JS::VM& vm, JS::Value value)
{
    // FIXME: De-duplicate this from the IDL generator.
    // 1. If the result of calling IsCallable(V) is false and the conversion to an IDL value is not being performed due to V being assigned to an attribute whose type is a nullable callback function that is annotated with [LegacyTreatNonObjectAsNull], then throw a TypeError.
    if (!value.is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, value.to_string_without_side_effects());

    // 2. Return the IDL callback function type value that represents a reference to the same object that V represents, with the incumbent settings object as the callback context.
    return vm.heap().allocate_without_realm<WebIDL::CallbackType>(value.as_object(), HTML::incumbent_settings_object());
}

// https://webidl.spec.whatwg.org/#es-sequence
static JS::ThrowCompletionOr<Vector<String>> convert_value_to_sequence_of_strings(JS::VM& vm, JS::Value value)
{
    // FIXME: De-duplicate this from the IDL generator.
    // An ECMAScript value V is converted to an IDL sequence<T> value as follows:
    // 1. If Type(V) is not Object, throw a TypeError.
    if (!value.is_object())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, value.to_string_without_side_effects());

    // 2. Let method be ? GetMethod(V, @@iterator).
    auto method = TRY(value.get_method(vm, vm.well_known_symbol_iterator()));

    // 3. If method is undefined, throw a TypeError.
    if (!method)
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotIterable, value.to_string_without_side_effects());

    // 4. Return the result of creating a sequence from V and method.

    // https://webidl.spec.whatwg.org/#create-sequence-from-iterable
    // To create an IDL value of type sequence<T> given an iterable iterable and an iterator getter method, perform the following steps:
    // 1. Let iter be ? GetIterator(iterable, sync, method).
    // FIXME: The WebIDL spec is out of date - it should be using GetIteratorFromMethod.
    auto iterator = TRY(JS::get_iterator_from_method(vm, value, *method));

    // 2. Initialize i to be 0.
    Vector<String> sequence_of_strings;

    // 3. Repeat
    for (;;) {
        // 1. Let next be ? IteratorStep(iter).
        auto next = TRY(JS::iterator_step(vm, iterator));

        // 2. If next is false, then return an IDL sequence value of type sequence<T> of length i, where the value of the element at index j is Sj.
        if (!next)
            return sequence_of_strings;

        // 3. Let nextItem be ? IteratorValue(next).
        auto next_item = TRY(JS::iterator_value(vm, *next));

        // 4. Initialize Si to the result of converting nextItem to an IDL value of type T.

        // https://webidl.spec.whatwg.org/#es-DOMString
        // An ECMAScript value V is converted to an IDL DOMString value by running the following algorithm:
        // 1. If V is null and the conversion is to an IDL type associated with the [LegacyNullToEmptyString] extended attribute, then return the DOMString value that represents the empty string.
        // NOTE: This doesn't apply.

        // 2. Let x be ? ToString(V).
        // 3. Return the IDL DOMString value that represents the same sequence of code units as the one the ECMAScript String value x represents.
        auto string_value = TRY(next_item.to_string(vm));

        sequence_of_strings.append(move(string_value));

        // 5. Set i to i + 1.
    }
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#dom-customelementregistry-define
JS::ThrowCompletionOr<void> CustomElementRegistry::define(String const& name, WebIDL::CallbackType* constructor, ElementDefinitionOptions options)
{
    auto& realm = this->realm();
    auto& vm = this->vm();

    // 1. If IsConstructor(constructor) is false, then throw a TypeError.
    if (!JS::Value(constructor->callback).is_constructor())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAConstructor, JS::Value(constructor->callback).to_string_without_side_effects());

    // 2. If name is not a valid custom element name, then throw a "SyntaxError" DOMException.
    if (!is_valid_custom_element_name(name))
        return JS::throw_completion(WebIDL::SyntaxError::create(realm, MUST(String::formatted("'{}' is not a valid custom element name"sv, name))));

    // 3. If this CustomElementRegistry contains an entry with name name, then throw a "NotSupportedError" DOMException.
    auto existing_definition_with_name_iterator = m_custom_element_definitions.find_if([&name](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->name() == name;
    });

    if (existing_definition_with_name_iterator != m_custom_element_definitions.end())
        return JS::throw_completion(WebIDL::NotSupportedError::create(realm, MUST(String::formatted("A custom element with name '{}' is already defined"sv, name))));

    // 4. If this CustomElementRegistry contains an entry with constructor constructor, then throw a "NotSupportedError" DOMException.
    auto existing_definition_with_constructor_iterator = m_custom_element_definitions.find_if([&constructor](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->constructor().callback == constructor->callback;
    });

    if (existing_definition_with_constructor_iterator != m_custom_element_definitions.end())
        return JS::throw_completion(WebIDL::NotSupportedError::create(realm, "The given constructor is already in use by another custom element"_fly_string));

    // 5. Let localName be name.
    String local_name = name;

    // 6. Let extends be the value of the extends member of options, or null if no such member exists.
    auto& extends = options.extends;

    // 7. If extends is not null, then:
    if (extends.has_value()) {
        // 1. If extends is a valid custom element name, then throw a "NotSupportedError" DOMException.
        if (is_valid_custom_element_name(extends.value()))
            return JS::throw_completion(WebIDL::NotSupportedError::create(realm, MUST(String::formatted("'{}' is a custom element name, only non-custom elements can be extended"sv, extends.value()))));

        // 2. If the element interface for extends and the HTML namespace is HTMLUnknownElement (e.g., if extends does not indicate an element definition in this specification), then throw a "NotSupportedError" DOMException.
        if (DOM::is_unknown_html_element(extends.value()))
            return JS::throw_completion(WebIDL::NotSupportedError::create(realm, MUST(String::formatted("'{}' is an unknown HTML element"sv, extends.value()))));

        // 3. Set localName to extends.
        local_name = extends.value();
    }

    // 8. If this CustomElementRegistry's element definition is running flag is set, then throw a "NotSupportedError" DOMException.
    if (m_element_definition_is_running)
        return JS::throw_completion(WebIDL::NotSupportedError::create(realm, "Cannot recursively define custom elements"_fly_string));

    // 9. Set this CustomElementRegistry's element definition is running flag.
    m_element_definition_is_running = true;

    // 10. Let formAssociated be false.
    bool form_associated = false;

    // 11. Let disableInternals be false.
    bool disable_internals = false;

    // 12. Let disableShadow be false.
    bool disable_shadow = false;

    // 13. Let observedAttributes be an empty sequence<DOMString>.
    Vector<String> observed_attributes;

    // NOTE: This is not in the spec, but is required because of how we catch the exception by using a lambda, meaning we need to define this variable outside of it to use it later.
    CustomElementDefinition::LifecycleCallbacksStorage lifecycle_callbacks;

    // 14. Run the following substeps while catching any exceptions:
    auto get_definition_attributes_from_constructor = [&]() -> JS::ThrowCompletionOr<void> {
        // 1. Let prototype be ? Get(constructor, "prototype").
        auto prototype_value = TRY(constructor->callback->get(vm.names.prototype));

        // 2. If Type(prototype) is not Object, then throw a TypeError exception.
        if (!prototype_value.is_object())
            return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, prototype_value.to_string_without_side_effects());

        auto& prototype = prototype_value.as_object();

        // 3. Let lifecycleCallbacks be a map with the keys "connectedCallback", "disconnectedCallback", "adoptedCallback", and "attributeChangedCallback", each of which belongs to an entry whose value is null.
        lifecycle_callbacks.set(CustomElementReactionNames::connectedCallback, {});
        lifecycle_callbacks.set(CustomElementReactionNames::disconnectedCallback, {});
        lifecycle_callbacks.set(CustomElementReactionNames::adoptedCallback, {});
        lifecycle_callbacks.set(CustomElementReactionNames::attributeChangedCallback, {});

        // 4. For each of the keys callbackName in lifecycleCallbacks, in the order listed in the previous step:
        for (auto const& callback_name : { CustomElementReactionNames::connectedCallback, CustomElementReactionNames::disconnectedCallback, CustomElementReactionNames::adoptedCallback, CustomElementReactionNames::attributeChangedCallback }) {
            // 1. Let callbackValue be ? Get(prototype, callbackName).
            auto callback_value = TRY(prototype.get(callback_name.to_deprecated_fly_string()));

            // 2. If callbackValue is not undefined, then set the value of the entry in lifecycleCallbacks with key callbackName to the result of converting callbackValue to the Web IDL Function callback type. Rethrow any exceptions from the conversion.
            if (!callback_value.is_undefined()) {
                auto callback = TRY(convert_value_to_callback_function(vm, callback_value));
                lifecycle_callbacks.set(callback_name, JS::make_handle(callback));
            }
        }

        // 5. If the value of the entry in lifecycleCallbacks with key "attributeChangedCallback" is not null, then:
        auto attribute_changed_callback_iterator = lifecycle_callbacks.find(CustomElementReactionNames::attributeChangedCallback);
        VERIFY(attribute_changed_callback_iterator != lifecycle_callbacks.end());
        if (!attribute_changed_callback_iterator->value.is_null()) {
            // 1. Let observedAttributesIterable be ? Get(constructor, "observedAttributes").
            auto observed_attributes_iterable = TRY(constructor->callback->get(JS::PropertyKey { "observedAttributes" }));

            // 2. If observedAttributesIterable is not undefined, then set observedAttributes to the result of converting observedAttributesIterable to a sequence<DOMString>. Rethrow any exceptions from the conversion.
            if (!observed_attributes_iterable.is_undefined())
                observed_attributes = TRY(convert_value_to_sequence_of_strings(vm, observed_attributes_iterable));
        }

        // 6. Let disabledFeatures be an empty sequence<DOMString>.
        Vector<String> disabled_features;

        // 7. Let disabledFeaturesIterable be ? Get(constructor, "disabledFeatures").
        auto disabled_features_iterable = TRY(constructor->callback->get(JS::PropertyKey { "disabledFeatures" }));

        // 8. If disabledFeaturesIterable is not undefined, then set disabledFeatures to the result of converting disabledFeaturesIterable to a sequence<DOMString>. Rethrow any exceptions from the conversion.
        if (!disabled_features_iterable.is_undefined())
            disabled_features = TRY(convert_value_to_sequence_of_strings(vm, disabled_features_iterable));

        // 9. Set disableInternals to true if disabledFeatures contains "internals".
        disable_internals = disabled_features.contains_slow("internals"sv);

        // 10. Set disableShadow to true if disabledFeatures contains "shadow".
        disable_shadow = disabled_features.contains_slow("shadow"sv);

        // 11. Let formAssociatedValue be ? Get( constructor, "formAssociated").
        auto form_associated_value = TRY(constructor->callback->get(JS::PropertyKey { "formAssociated" }));

        // 12. Set formAssociated to the result of converting formAssociatedValue to a boolean. Rethrow any exceptions from the conversion.
        // NOTE: Converting to a boolean cannot throw with ECMAScript.

        // https://webidl.spec.whatwg.org/#es-boolean
        // An ECMAScript value V is converted to an IDL boolean value by running the following algorithm:
        // 1. Let x be the result of computing ToBoolean(V).
        // 2. Return the IDL boolean value that is the one that represents the same truth value as the ECMAScript Boolean value x.
        form_associated = form_associated_value.to_boolean();

        // 13. If formAssociated is true, for each of "formAssociatedCallback", "formResetCallback", "formDisabledCallback", and "formStateRestoreCallback" callbackName:
        if (form_associated) {
            for (auto const& callback_name : { CustomElementReactionNames::formAssociatedCallback, CustomElementReactionNames::formResetCallback, CustomElementReactionNames::formDisabledCallback, CustomElementReactionNames::formStateRestoreCallback }) {
                // 1. Let callbackValue be ? Get(prototype, callbackName).
                auto callback_value = TRY(prototype.get(callback_name.to_deprecated_fly_string()));

                // 2. If callbackValue is not undefined, then set the value of the entry in lifecycleCallbacks with key callbackName to the result of converting callbackValue to the Web IDL Function callback type. Rethrow any exceptions from the conversion.
                if (!callback_value.is_undefined())
                    lifecycle_callbacks.set(callback_name, JS::make_handle(TRY(convert_value_to_callback_function(vm, callback_value))));
            }
        }

        return {};
    };

    auto maybe_exception = get_definition_attributes_from_constructor();

    // Then, perform the following substep, regardless of whether the above steps threw an exception or not:
    // 1. Unset this CustomElementRegistry's element definition is running flag.
    m_element_definition_is_running = false;

    // Finally, if the first set of substeps threw an exception, then rethrow that exception (thus terminating this algorithm). Otherwise, continue onward.
    if (maybe_exception.is_throw_completion())
        return maybe_exception.release_error();

    // 15. Let definition be a new custom element definition with name name, local name localName, constructor constructor, observed attributes observedAttributes, lifecycle callbacks lifecycleCallbacks, form-associated formAssociated, disable internals disableInternals, and disable shadow disableShadow.
    auto definition = CustomElementDefinition::create(realm, name, local_name, *constructor, move(observed_attributes), move(lifecycle_callbacks), form_associated, disable_internals, disable_shadow);

    // 16. Add definition to this CustomElementRegistry.
    m_custom_element_definitions.append(JS::make_handle(*definition));

    // 17. Let document be this CustomElementRegistry's relevant global object's associated Document.
    auto& document = verify_cast<HTML::Window>(relevant_global_object(*this)).associated_document();

    // 18. Let upgrade candidates be all elements that are shadow-including descendants of document, whose namespace is the HTML namespace and whose local name is localName, in shadow-including tree order.
    //     Additionally, if extends is non-null, only include elements whose is value is equal to name.
    Vector<JS::Handle<DOM::Element>> upgrade_candidates;

    document.for_each_shadow_including_descendant([&](DOM::Node& inclusive_descendant) {
        if (!is<DOM::Element>(inclusive_descendant))
            return IterationDecision::Continue;

        auto& inclusive_descendant_element = static_cast<DOM::Element&>(inclusive_descendant);

        if (inclusive_descendant_element.namespace_uri() == Namespace::HTML && inclusive_descendant_element.local_name() == local_name && (!extends.has_value() || inclusive_descendant_element.is_value() == name))
            upgrade_candidates.append(JS::make_handle(inclusive_descendant_element));

        return IterationDecision::Continue;
    });

    // 19. For each element element in upgrade candidates, enqueue a custom element upgrade reaction given element and definition.
    for (auto& element : upgrade_candidates)
        element->enqueue_a_custom_element_upgrade_reaction(definition);

    // 20. If this CustomElementRegistry's when-defined promise map contains an entry with key name:
    auto promise_when_defined_iterator = m_when_defined_promise_map.find(name);
    if (promise_when_defined_iterator != m_when_defined_promise_map.end()) {
        // 1. Let promise be the value of that entry.
        auto* promise = promise_when_defined_iterator->value.cell();

        // 2. Resolve promise with constructor.
        promise->fulfill(constructor->callback);

        // 3. Delete the entry with key name from this CustomElementRegistry's when-defined promise map.
        m_when_defined_promise_map.remove(name);
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#dom-customelementregistry-get
Variant<JS::Handle<WebIDL::CallbackType>, JS::Value> CustomElementRegistry::get(String const& name) const
{
    // 1. If this CustomElementRegistry contains an entry with name name, then return that entry's constructor.
    auto existing_definition_iterator = m_custom_element_definitions.find_if([&name](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->name() == name;
    });

    if (!existing_definition_iterator.is_end())
        return JS::make_handle(existing_definition_iterator->cell()->constructor());

    // 2. Otherwise, return undefined.
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#dom-customelementregistry-whendefined
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> CustomElementRegistry::when_defined(String const& name)
{
    auto& realm = this->realm();

    // 1. If name is not a valid custom element name, then return a new promise rejected with a "SyntaxError" DOMException.
    if (!is_valid_custom_element_name(name)) {
        auto promise = JS::Promise::create(realm);
        promise->reject(WebIDL::SyntaxError::create(realm, MUST(String::formatted("'{}' is not a valid custom element name"sv, name))));
        return promise;
    }

    // 2. If this CustomElementRegistry contains an entry with name name, then return a new promise resolved with that entry's constructor.
    auto existing_definition_iterator = m_custom_element_definitions.find_if([&name](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->name() == name;
    });

    if (existing_definition_iterator != m_custom_element_definitions.end()) {
        auto promise = JS::Promise::create(realm);
        promise->fulfill(existing_definition_iterator->cell()->constructor().callback);
        return promise;
    }

    // 3. Let map be this CustomElementRegistry's when-defined promise map.
    // NOTE: Not necessary.

    // 4. If map does not contain an entry with key name, create an entry in map with key name and whose value is a new promise.
    // 5. Let promise be the value of the entry in map with key name.
    JS::GCPtr<JS::Promise> promise;

    auto existing_promise_iterator = m_when_defined_promise_map.find(name);
    if (existing_promise_iterator != m_when_defined_promise_map.end()) {
        promise = existing_promise_iterator->value.cell();
    } else {
        promise = JS::Promise::create(realm);
        m_when_defined_promise_map.set(name, JS::make_handle(promise));
    }

    // 5. Return promise.
    VERIFY(promise);
    return JS::NonnullGCPtr { *promise };
}

// https://html.spec.whatwg.org/multipage/custom-elements.html#dom-customelementregistry-upgrade
void CustomElementRegistry::upgrade(JS::NonnullGCPtr<DOM::Node> root) const
{
    // 1. Let candidates be a list of all of root's shadow-including inclusive descendant elements, in shadow-including tree order.
    Vector<JS::Handle<DOM::Element>> candidates;

    root->for_each_shadow_including_inclusive_descendant([&](DOM::Node& inclusive_descendant) {
        if (!is<DOM::Element>(inclusive_descendant))
            return IterationDecision::Continue;

        auto& inclusive_descendant_element = static_cast<DOM::Element&>(inclusive_descendant);
        candidates.append(JS::make_handle(inclusive_descendant_element));

        return IterationDecision::Continue;
    });

    // 2. For each candidate of candidates, try to upgrade candidate.
    for (auto& candidate : candidates)
        candidate->try_to_upgrade();
}

JS::GCPtr<CustomElementDefinition> CustomElementRegistry::get_definition_with_name_and_local_name(String const& name, String const& local_name) const
{
    auto definition_iterator = m_custom_element_definitions.find_if([&](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->name() == name && definition->local_name() == local_name;
    });

    return definition_iterator.is_end() ? nullptr : definition_iterator->ptr();
}

JS::GCPtr<CustomElementDefinition> CustomElementRegistry::get_definition_from_new_target(JS::FunctionObject const& new_target) const
{
    auto definition_iterator = m_custom_element_definitions.find_if([&](JS::Handle<CustomElementDefinition> const& definition) {
        return definition->constructor().callback.ptr() == &new_target;
    });

    return definition_iterator.is_end() ? nullptr : definition_iterator->ptr();
}

}
