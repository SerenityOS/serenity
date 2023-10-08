/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ValueInlines.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HTMLOptionElementPrototype.h>
#include <LibWeb/Bindings/OptionConstructor.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::Bindings {

OptionConstructor::OptionConstructor(JS::Realm& realm)
    : NativeFunction(realm.intrinsics().function_prototype())
{
}

void OptionConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);

    define_direct_property(vm.names.prototype, &ensure_web_prototype<Bindings::HTMLOptionElementPrototype>(realm, "HTMLOptionElement"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

JS::ThrowCompletionOr<JS::Value> OptionConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "Option");
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Object>> OptionConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    // 1. Let document be the current global object's associated Document.
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    auto& document = window.associated_document();

    // 2. Let option be the result of creating an element given document, option, and the HTML namespace.
    auto element = TRY(Bindings::throw_dom_exception_if_needed(vm, [&]() { return DOM::create_element(document, HTML::TagNames::option, Namespace::HTML); }));
    JS::NonnullGCPtr<HTML::HTMLOptionElement> option_element = verify_cast<HTML::HTMLOptionElement>(*element);

    // 3. If text is not the empty string, then append to option a new Text node whose data is text.
    if (vm.argument_count() > 0) {
        auto text = TRY(vm.argument(0).to_string(vm));
        if (!text.is_empty()) {
            auto new_text_node = vm.heap().allocate<DOM::Text>(realm, document, text);
            MUST(option_element->append_child(*new_text_node));
        }
    }

    // 4. If value is given, then set an attribute value for option using "value" and value.
    if (vm.argument_count() > 1) {
        auto value = TRY(vm.argument(1).to_string(vm));
        MUST(option_element->set_attribute(HTML::AttributeNames::value, value));
    }

    // 5. If defaultSelected is true, then set an attribute value for option using "selected" and the empty string.
    if (vm.argument_count() > 2) {
        auto default_selected = vm.argument(2).to_boolean();
        if (default_selected) {
            MUST(option_element->set_attribute(HTML::AttributeNames::selected, String {}));
        }
    }

    // 6. If selected is true, then set option's selectedness to true; otherwise set its selectedness to false (even if defaultSelected is true).
    option_element->m_selected = vm.argument(3).to_boolean();

    // 7. Return option.
    return option_element;
}

}
