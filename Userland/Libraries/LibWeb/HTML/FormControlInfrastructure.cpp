/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/FormControlInfrastructure.h>
#include <LibWeb/HTML/FormDataEvent.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLDataListElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#create-an-entry
WebIDL::ExceptionOr<XHR::FormDataEntry> create_entry(JS::Realm& realm, String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename)
{
    auto& vm = realm.vm();

    // 1. Set name to the result of converting name into a scalar value string.
    auto entry_name = TRY_OR_THROW_OOM(vm, Infra::convert_to_scalar_value_string(name));

    auto entry_value = TRY(value.visit(
        // 2. If value is a string, then set value to the result of converting value into a scalar value string.
        [&](String const& string) -> WebIDL::ExceptionOr<Variant<JS::Handle<FileAPI::File>, String>> {
            return TRY_OR_THROW_OOM(vm, Infra::convert_to_scalar_value_string(string));
        },
        // 3. Otherwise:
        [&](JS::NonnullGCPtr<FileAPI::Blob> const& blob) -> WebIDL::ExceptionOr<Variant<JS::Handle<FileAPI::File>, String>> {
            // 1. If value is not a File object, then set value to a new File object, representing the same bytes, whose name attribute value is "blob".
            // 2. If filename is given, then set value to a new File object, representing the same bytes, whose name attribute is filename.
            String name_attribute;
            if (filename.has_value())
                name_attribute = filename.value();
            else
                name_attribute = TRY_OR_THROW_OOM(vm, "blob"_string);
            return JS::make_handle(TRY(FileAPI::File::create(realm, { JS::make_handle(*blob) }, move(name_attribute), {})));
        }));

    // 4. Return an entry whose name is name and whose value is value.
    return XHR::FormDataEntry {
        .name = move(entry_name),
        .value = move(entry_value),
    };
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#constructing-the-form-data-set
// FIXME: Add missing parameters optional submitter, and optional encoding
WebIDL::ExceptionOr<Optional<Vector<XHR::FormDataEntry>>> construct_entry_list(JS::Realm& realm, HTMLFormElement& form)
{
    auto& vm = realm.vm();

    // 1. If form's constructing entry list is true, then return null.
    if (form.constructing_entry_list())
        return Optional<Vector<XHR::FormDataEntry>> {};

    // 2. Set form's constructing entry list to true.
    form.set_constructing_entry_list(true);

    // 3. Let controls be a list of all the submittable elements whose form owner is form, in tree order.
    auto controls = TRY_OR_THROW_OOM(vm, form.get_submittable_elements());

    // 4. Let entry list be a new empty entry list.
    Vector<XHR::FormDataEntry> entry_list;

    // 5. For each element field in controls, in tree order:
    for (auto const& control : controls) {
        // 1. If any of the following is true, then continue:
        // - The field element has a datalist element ancestor.
        if (control->first_ancestor_of_type<HTML::HTMLDataListElement>())
            continue;
        // - The field element is disabled.
        if (control->is_actually_disabled())
            continue;
        // FIXME: - The field element is a button but it is not submitter.
        // - The field element is an input element whose type attribute is in the Checkbox state and whose checkedness is false.
        // - The field element is an input element whose type attribute is in the Radio Button state and whose checkedness is false.
        if (auto* input_element = dynamic_cast<HTML::HTMLInputElement*>(control.ptr())) {
            if ((input_element->type() == "checkbox" || input_element->type() == "radio") && !input_element->checked())
                continue;
        }

        // 2. If the field element is an input element whose type attribute is in the Image Button state, then:
        if (auto* input_element = dynamic_cast<HTML::HTMLInputElement*>(control.ptr()); input_element && input_element->type() == "image") {
            // FIXME: 1. If the field element has a name attribute specified and its value is not the empty string, let name be that value followed by a single U+002E FULL STOP character (.). Otherwise, let name be the empty string.
            // FIXME: 2. Let namex be the string consisting of the concatenation of name and a single U0078 LATIN SMALL LETTER X character (x).
            // FIXME: 3. Let namey be the string consisting of the concatenation of name and a single U+0079 LATIN SMALL LETTER Y character (y).
            // FIXME: 4. The field element is submitter, and before this algorithm was invoked the user indicated a coordinate. Let x be the x-component of the coordinate selected by the user, and let y be the y-component of the coordinate selected by the user.
            // FIXME: 5. Create an entry with namex and x, and append it to entry list.
            // FIXME: 6. Create an entry with namey and y, and append it to entry list.
            // 7. Continue.
            continue;
        }

        // FIXME: 3. If the field is a form-associated custom element, then perform the entry construction algorithm given field and entry list, then continue.

        // 4. If either the field element does not have a name attribute specified, or its name attribute's value is the empty string, then continue.
        if (control->name().is_empty())
            continue;

        // 5. Let name be the value of the field element's name attribute.
        auto name = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(control->name()));

        // 6. If the field element is a select element, then for each option element in the select element's list of options whose selectedness is true and that is not disabled, create an entry with name and the value of the option element, and append it to entry list.
        if (auto* select_element = dynamic_cast<HTML::HTMLSelectElement*>(control.ptr())) {
            for (auto const& option_element : select_element->list_of_options()) {
                if (option_element->selected() && !option_element->disabled()) {
                    auto option_name = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(option_element->name()));
                    auto option_value = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(option_element->value()));
                    TRY_OR_THROW_OOM(vm, entry_list.try_append(XHR::FormDataEntry { .name = move(option_name), .value = move(option_value) }));
                }
            }
        }
        // 7. Otherwise, if the field element is an input element whose type attribute is in the Checkbox state or the Radio Button state, then:
        else if (auto* checkbox_or_radio_element = dynamic_cast<HTML::HTMLInputElement*>(control.ptr()); checkbox_or_radio_element && (checkbox_or_radio_element->type() == "checkbox" || checkbox_or_radio_element->type() == "radio") && checkbox_or_radio_element->checked()) {
            //  1. If the field element has a value attribute specified, then let value be the value of that attribute; otherwise, let value be the string "on".
            auto value = checkbox_or_radio_element->value();
            if (value.is_empty())
                value = "on";

            // 2. Create an entry with name and value, and append it to entry list.
            auto checkbox_or_radio_element_name = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(checkbox_or_radio_element->name()));
            auto checkbox_or_radio_element_value = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(value));
            TRY_OR_THROW_OOM(vm, entry_list.try_append(XHR::FormDataEntry { .name = move(checkbox_or_radio_element_name), .value = move(checkbox_or_radio_element_value) }));
        }
        // 8. Otherwise, if the field element is an input element whose type attribute is in the File Upload state, then:
        else if (auto* file_element = dynamic_cast<HTML::HTMLInputElement*>(control.ptr()); file_element && file_element->type() == "file") {
            // 1. If there are no selected files, then create an entry with name and a new File object with an empty name, application/octet-stream as type, and an empty body, and append it to entry list.
            if (file_element->files()->length() == 0) {
                FileAPI::FilePropertyBag options {};
                options.type = TRY_OR_THROW_OOM(vm, "application/octet-stream"_string);
                auto file = TRY(FileAPI::File::create(realm, {}, String {}, options));
                TRY_OR_THROW_OOM(vm, entry_list.try_append(XHR::FormDataEntry { .name = move(name), .value = JS::make_handle(file) }));
            }
            // 2. Otherwise, for each file in selected files, create an entry with name and a File object representing the file, and append it to entry list.
            else {
                for (size_t i = 0; i < file_element->files()->length(); i++) {
                    auto file = JS::NonnullGCPtr { *file_element->files()->item(i) };
                    TRY_OR_THROW_OOM(vm, entry_list.try_append(XHR::FormDataEntry { .name = move(name), .value = JS::make_handle(file) }));
                }
            }
        }
        // FIXME: 9. Otherwise, if the field element is an input element whose type attribute is in the Hidden state and name is an ASCII case-insensitive match for "_charset_":
        // FIXME:    1. Let charset be the name of encoding if encoding is given, and "UTF-8" otherwise.
        // FIXME:    2. Create an entry with name and charset, and append it to entry list.
        // 10. Otherwise, create an entry with name and the value of the field element, and append it to entry list.
        else {
            auto* element = dynamic_cast<HTML::HTMLElement*>(control.ptr());
            VERIFY(element);
            auto value_attribute = TRY_OR_THROW_OOM(vm, String::from_deprecated_string(element->attribute("value"sv)));
            TRY_OR_THROW_OOM(vm, entry_list.try_append(XHR::FormDataEntry { .name = move(name), .value = move(value_attribute) }));
        }

        // FIXME: 11. If the element has a dirname attribute, and that attribute's value is not the empty string, then:
        // FIXME:     1. Let dirname be the value of the element's dirname attribute.
        // FIXME:     2. Let dir be the string "ltr" if the directionality of the element is 'ltr', and "rtl" otherwise (i.e., when the directionality of the element is 'rtl').
        // FIXME:     3. Create an entry with dirname and dir, and append it to entry list.
    }
    // 6. Let form data be a new FormData object associated with entry list.
    auto form_data = TRY(XHR::FormData::construct_impl(realm, entry_list));

    // 7. Fire an event named formdata at form using FormDataEvent, with the formData attribute initialized to form data and the bubbles attribute initialized to true.
    FormDataEventInit init {};
    init.form_data = form_data;
    auto form_data_event = TRY(FormDataEvent::construct_impl(realm, String::from_deprecated_string(HTML::EventNames::formdata).release_value_but_fixme_should_propagate_errors(), init));
    form_data_event->set_bubbles(true);
    form.dispatch_event(form_data_event);

    // 8. Set form's constructing entry list to false.
    form.set_constructing_entry_list(false);

    // 9. Return a clone of entry list.
    return entry_list;
}

}
