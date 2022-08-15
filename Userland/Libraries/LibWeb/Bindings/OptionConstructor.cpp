/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLOptionElementPrototype.h>
#include <LibWeb/Bindings/HTMLOptionElementWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/OptionConstructor.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::Bindings {

OptionConstructor::OptionConstructor(JS::Realm& realm)
    : NativeFunction(*realm.global_object().function_prototype())
{
}

void OptionConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(realm.global_object());
    NativeFunction::initialize(realm);

    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<HTMLOptionElementPrototype>("HTMLOptionElement"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

JS::ThrowCompletionOr<JS::Value> OptionConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Option");
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option
JS::ThrowCompletionOr<JS::Object*> OptionConstructor::construct(FunctionObject&)
{
    // 1. Let document be the current global object's associated Document.
    auto& window = static_cast<WindowObject&>(HTML::current_global_object());
    auto& document = window.impl().associated_document();

    // 2. Let option be the result of creating an element given document, option, and the HTML namespace.
    auto option_element = static_ptr_cast<HTML::HTMLOptionElement>(DOM::create_element(document, HTML::TagNames::option, Namespace::HTML));

    // 3. If text is not the empty string, then append to option a new Text node whose data is text.
    if (vm().argument_count() > 0) {
        auto text = TRY(vm().argument(0).to_string(global_object()));
        if (!text.is_empty()) {
            auto new_text_node = adopt_ref(*new DOM::Text(document, text));
            option_element->append_child(new_text_node);
        }
    }

    // 4. If value is given, then set an attribute value for option using "value" and value.
    if (vm().argument_count() > 1) {
        auto value = TRY(vm().argument(1).to_string(global_object()));
        option_element->set_attribute(HTML::AttributeNames::value, value);
    }

    // 5. If defaultSelected is true, then set an attribute value for option using "selected" and the empty string.
    if (vm().argument_count() > 2) {
        auto default_selected = vm().argument(2).to_boolean();
        if (default_selected) {
            option_element->set_attribute(HTML::AttributeNames::selected, "");
        }
    }

    // 6. If selected is true, then set option's selectedness to true; otherwise set its selectedness to false (even if defaultSelected is true).
    option_element->m_selected = vm().argument(3).to_boolean();

    // 7. Return option.
    return wrap(global_object(), option_element);
}

}
