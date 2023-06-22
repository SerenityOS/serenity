/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLFormControlsCollection.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/FormControlInfrastructure.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLObjectElement.h>
#include <LibWeb/HTML/HTMLOutputElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/HTML/SubmitEvent.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/URL/URL.h>

namespace Web::HTML {

HTMLFormElement::HTMLFormElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFormElement::~HTMLFormElement() = default;

void HTMLFormElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLFormElementPrototype>(realm, "HTMLFormElement"));
}

void HTMLFormElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_elements);
    for (auto& element : m_associated_elements)
        visitor.visit(element.ptr());
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-form-submit
WebIDL::ExceptionOr<void> HTMLFormElement::submit_form(JS::NonnullGCPtr<HTMLElement> submitter, bool from_submit_binding)
{
    auto& vm = this->vm();
    auto& realm = this->realm();

    // 1. If form cannot navigate, then return.
    if (cannot_navigate())
        return {};

    // 2. If form's constructing entry list is true, then return.
    if (m_constructing_entry_list)
        return {};

    // 3. Let form document be form's node document.
    JS::NonnullGCPtr<DOM::Document> form_document = this->document();

    // 4. If form document's active sandboxing flag set has its sandboxed forms browsing context flag set, then return.
    if (has_flag(form_document->active_sandboxing_flag_set(), HTML::SandboxingFlagSet::SandboxedForms))
        return {};

    // 5. If the submitted from submit() method flag is not set, then:
    if (!from_submit_binding) {
        // 1. If form's firing submission events is true, then return.
        if (m_firing_submission_events)
            return {};

        // 2. Set form's firing submission events to true.
        m_firing_submission_events = true;

        // FIXME: 3. If the submitter element's no-validate state is false, then interactively validate the constraints
        //           of form and examine the result. If the result is negative (i.e., the constraint validation concluded
        //           that there were invalid fields and probably informed the user of this), then:
        //           1. Set form's firing submission events to false.
        //           2. Return.

        // 4. Let submitterButton be null if submitter is form. Otherwise, let submitterButton be submitter.
        JS::GCPtr<HTMLElement> submitter_button;
        if (submitter != this)
            submitter_button = submitter;

        // 5. Let shouldContinue be the result of firing an event named submit at form using SubmitEvent, with the
        //    submitter attribute initialized to submitterButton, the bubbles attribute initialized to true, and the
        //    cancelable attribute initialized to true.
        SubmitEventInit event_init {};
        event_init.submitter = submitter_button;
        auto submit_event = SubmitEvent::create(realm, EventNames::submit, event_init);
        submit_event->set_bubbles(true);
        submit_event->set_cancelable(true);
        bool should_continue = dispatch_event(*submit_event);

        // 6. Set form's firing submission events to false.
        m_firing_submission_events = false;

        // 7. If shouldContinue is false, then return.
        if (!should_continue)
            return {};

        // 8. If form cannot navigate, then return.
        // Spec Note: Cannot navigate is run again as dispatching the submit event could have changed the outcome.
        if (cannot_navigate())
            return {};
    }

    // 6. Let encoding be the result of picking an encoding for the form.
    auto encoding = TRY_OR_THROW_OOM(vm, pick_an_encoding());
    if (encoding != "UTF-8"sv) {
        dbgln("FIXME: Support encodings other than UTF-8 in form submission. Returning from form submission.");
        return {};
    }

    // 7. Let entry list be the result of constructing the entry list with form, submitter, and encoding.
    auto entry_list_or_null = TRY(construct_entry_list(realm, *this, submitter, encoding));

    // 8. Assert: entry list is not null.
    VERIFY(entry_list_or_null.has_value());
    auto entry_list = entry_list_or_null.release_value();

    // 9. If form cannot navigate, then return.
    // Spec Note: Cannot navigate is run again as dispatching the formdata event in constructing the entry list could
    //            have changed the outcome.
    if (cannot_navigate())
        return {};

    // 10. Let method be the submitter element's method.
    auto method = method_state_from_form_element(submitter);

    // 11. If method is dialog, then:
    if (method == MethodAttributeState::Dialog) {
        // FIXME: 1. If form does not have an ancestor dialog element, then return.
        // FIXME: 2. Let subject be form's nearest ancestor dialog element.
        // FIXME: 3. Let result be null.
        // FIXME: 4. If submitter is an input element whose type attribute is in the Image Button state, then:
        //           1. Let (x, y) be the selected coordinate.
        //           2. Set result to the concatenation of x, ",", and y.
        // FIXME: 5. Otherwise, if submitter has a value, then set result to that value.
        // FIXME: 6. Close the dialog subject with result.
        // FIXME: 7. Return.

        dbgln("FIXME: Implement form submission with `dialog` action. Returning from form submission.");
        return {};
    }

    // 12. Let action be the submitter element's action.
    auto action = action_from_form_element(submitter);

    // 13. If action is the empty string, let action be the URL of the form document.
    if (action.is_empty())
        action = form_document->url_string();

    // 14. Parse a URL given action, relative to the submitter element's node document. If this fails, return.
    // 15. Let parsed action be the resulting URL record.
    auto parsed_action = document().parse_url(action);
    if (!parsed_action.is_valid()) {
        dbgln("Failed to submit form: Invalid URL: {}", action);
        return {};
    }

    // 16. Let scheme be the scheme of parsed action.
    auto const& scheme = parsed_action.scheme();

    // 17. Let enctype be the submitter element's enctype.
    auto encoding_type = encoding_type_state_from_form_element(submitter);

    // 18. Let target be the submitter element's formtarget attribute value, if the element is a submit button and has
    //     such an attribute. Otherwise, let it be the result of getting an element's target given submitter's form
    //     owner.
    DeprecatedString target;
    if (submitter->has_attribute(AttributeNames::formtarget))
        target = submitter->deprecated_attribute(AttributeNames::formtarget);
    else
        target = get_an_elements_target();

    // 19. Let noopener be the result of getting an element's noopener with form and target.
    auto no_opener = get_an_elements_noopener(target);

    // 20. Let targetNavigable be the first return value of applying the rules for choosing a navigable given target, form's node navigable, and noopener.
    auto target_navigable = form_document->navigable()->choose_a_navigable(target, no_opener).navigable;

    // 21. If targetNavigable is null, then return.
    if (!target_navigable) {
        dbgln("Failed to submit form: choose_a_browsing_context returning a null browsing context");
        return {};
    }

    // 22. Let historyHandling be "push".
    // NOTE: This is `Default` in the old spec.
    auto history_handling = HistoryHandlingBehavior::Default;

    // 23. If form document has not yet completely loaded, then set historyHandling to "replace".
    if (!form_document->is_completely_loaded())
        history_handling = HistoryHandlingBehavior::Replace;

    // 24. Select the appropriate row in the table below based on scheme as given by the first cell of each row.
    //     Then, select the appropriate cell on that row based on method as given in the first cell of each column.
    //     Then, jump to the steps named in that cell and defined below the table.

    // 	          | GET 	      | POST
    // ------------------------------------------------------
    // http 	  | Mutate action URL |	Submit as entity body
    // https 	  | Mutate action URL |	Submit as entity body
    // ftp 	  | Get action URL    |	Get action URL
    // javascript | Get action URL    |	Get action URL
    // data 	  | Mutate action URL |	Get action URL
    // mailto 	  | Mail with headers |	Mail as body

    // If scheme is not one of those listed in this table, then the behavior is not defined by this specification.
    // User agents should, in the absence of another specification defining this, act in a manner analogous to that defined
    // in this specification for similar schemes.

    // This should have been handled above.
    VERIFY(method != MethodAttributeState::Dialog);

    if (scheme.is_one_of("http"sv, "https"sv)) {
        if (method == MethodAttributeState::GET)
            TRY_OR_THROW_OOM(vm, mutate_action_url(move(parsed_action), move(entry_list), move(encoding), *target_navigable, history_handling));
        else
            TRY_OR_THROW_OOM(vm, submit_as_entity_body(move(parsed_action), move(entry_list), encoding_type, move(encoding), *target_navigable, history_handling));
    } else if (scheme.is_one_of("ftp"sv, "javascript"sv)) {
        get_action_url(move(parsed_action), *target_navigable, history_handling);
    } else if (scheme == "data"sv) {
        if (method == MethodAttributeState::GET)
            TRY_OR_THROW_OOM(vm, mutate_action_url(move(parsed_action), move(entry_list), move(encoding), *target_navigable, history_handling));
        else
            get_action_url(move(parsed_action), *target_navigable, history_handling);
    } else if (scheme == "mailto"sv) {
        if (method == MethodAttributeState::GET)
            TRY_OR_THROW_OOM(vm, mail_with_headers(move(parsed_action), move(entry_list), move(encoding), *target_navigable, history_handling));
        else
            TRY_OR_THROW_OOM(vm, mail_as_body(move(parsed_action), move(entry_list), encoding_type, move(encoding), *target_navigable, history_handling));
    } else {
        dbgln("Failed to submit form: Unknown scheme: {}", scheme);
        return {};
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#resetting-a-form
void HTMLFormElement::reset_form()
{
    // 1. Let reset be the result of firing an event named reset at form, with the bubbles and cancelable attributes initialized to true.
    auto reset_event = DOM::Event::create(realm(), HTML::EventNames::reset);
    reset_event->set_bubbles(true);
    reset_event->set_cancelable(true);

    bool reset = dispatch_event(reset_event);

    // 2. If reset is true, then invoke the reset algorithm of each resettable element whose form owner is form.
    if (reset) {
        for (auto element : m_associated_elements) {
            VERIFY(is<FormAssociatedElement>(*element));
            auto& form_associated_element = dynamic_cast<FormAssociatedElement&>(*element);
            if (form_associated_element.is_resettable())
                form_associated_element.reset_algorithm();
        }
    }
}

WebIDL::ExceptionOr<void> HTMLFormElement::submit()
{
    return submit_form(*this, true);
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-reset
void HTMLFormElement::reset()
{
    // 1. If the form element is marked as locked for reset, then return.
    if (m_locked_for_reset)
        return;

    // 2. Mark the form element as locked for reset.
    m_locked_for_reset = true;

    // 3. Reset the form element.
    reset_form();

    // 4. Unmark the form element as locked for reset.
    m_locked_for_reset = false;
}

void HTMLFormElement::add_associated_element(Badge<FormAssociatedElement>, HTMLElement& element)
{
    m_associated_elements.append(element);
}

void HTMLFormElement::remove_associated_element(Badge<FormAssociatedElement>, HTMLElement& element)
{
    m_associated_elements.remove_first_matching([&](auto& entry) { return entry.ptr() == &element; });
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fs-action
DeprecatedString HTMLFormElement::action_from_form_element(JS::NonnullGCPtr<HTMLElement> element) const
{
    // The action of an element is the value of the element's formaction attribute, if the element is a submit button
    // and has such an attribute, or the value of its form owner's action attribute, if it has one, or else the empty
    // string.
    if (auto const* form_associated_element = dynamic_cast<FormAssociatedElement const*>(element.ptr());
        form_associated_element && form_associated_element->is_submit_button() && element->has_attribute(AttributeNames::formaction))
        return deprecated_attribute(AttributeNames::formaction);

    if (this->has_attribute(AttributeNames::action))
        return deprecated_attribute(AttributeNames::action);

    return DeprecatedString::empty();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#form-submission-attributes:attr-fs-method-2
static HTMLFormElement::MethodAttributeState method_attribute_to_method_state(StringView method)
{
#define __ENUMERATE_FORM_METHOD_ATTRIBUTE(keyword, state)             \
    if (Infra::is_ascii_case_insensitive_match(#keyword##sv, method)) \
        return HTMLFormElement::MethodAttributeState::state;
    ENUMERATE_FORM_METHOD_ATTRIBUTES
#undef __ENUMERATE_FORM_METHOD_ATTRIBUTE

    // The method attribute's invalid value default and missing value default are both the GET state.
    return HTMLFormElement::MethodAttributeState::GET;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fs-method
HTMLFormElement::MethodAttributeState HTMLFormElement::method_state_from_form_element(JS::NonnullGCPtr<HTMLElement const> element) const
{
    // If the element is a submit button and has a formmethod attribute, then the element's method is that attribute's state;
    // otherwise, it is the form owner's method attribute's state.
    if (auto const* form_associated_element = dynamic_cast<FormAssociatedElement const*>(element.ptr());
        form_associated_element && form_associated_element->is_submit_button() && element->has_attribute(AttributeNames::formmethod)) {
        // NOTE: `formmethod` is the same as `method`, except that it has no missing value default.
        //       This is handled by not calling `method_attribute_to_method_state` in the first place if there is no `formmethod` attribute.
        return method_attribute_to_method_state(element->deprecated_attribute(AttributeNames::formmethod));
    }

    if (!this->has_attribute(AttributeNames::method))
        return MethodAttributeState::GET;

    return method_attribute_to_method_state(this->deprecated_attribute(AttributeNames::method));
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#form-submission-attributes:attr-fs-enctype-2
static HTMLFormElement::EncodingTypeAttributeState encoding_type_attribute_to_encoding_type_state(StringView encoding_type)
{
#define __ENUMERATE_FORM_METHOD_ENCODING_TYPE(keyword, state)               \
    if (Infra::is_ascii_case_insensitive_match(keyword##sv, encoding_type)) \
        return HTMLFormElement::EncodingTypeAttributeState::state;
    ENUMERATE_FORM_METHOD_ENCODING_TYPES
#undef __ENUMERATE_FORM_METHOD_ENCODING_TYPE

    // The enctype attribute's invalid value default and missing value default are both the application/x-www-form-urlencoded state.
    return HTMLFormElement::EncodingTypeAttributeState::FormUrlEncoded;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fs-enctype
HTMLFormElement::EncodingTypeAttributeState HTMLFormElement::encoding_type_state_from_form_element(JS::NonnullGCPtr<HTMLElement> element) const
{
    // If the element is a submit button and has a formenctype attribute, then the element's enctype is that attribute's state;
    // otherwise, it is the form owner's enctype attribute's state.
    if (auto const* form_associated_element = dynamic_cast<FormAssociatedElement const*>(element.ptr());
        form_associated_element && form_associated_element->is_submit_button() && element->has_attribute(AttributeNames::formenctype)) {
        // NOTE: `formenctype` is the same as `enctype`, except that it has no missing value default.
        //       This is handled by not calling `encoding_type_attribute_to_encoding_type_state` in the first place if there is no
        //       `formenctype` attribute.
        return encoding_type_attribute_to_encoding_type_state(element->deprecated_attribute(AttributeNames::formenctype));
    }

    if (!this->has_attribute(AttributeNames::enctype))
        return EncodingTypeAttributeState::FormUrlEncoded;

    return encoding_type_attribute_to_encoding_type_state(this->deprecated_attribute(AttributeNames::enctype));
}

static bool is_form_control(DOM::Element const& element)
{
    if (is<HTMLButtonElement>(element)
        || is<HTMLFieldSetElement>(element)
        || is<HTMLObjectElement>(element)
        || is<HTMLOutputElement>(element)
        || is<HTMLSelectElement>(element)
        || is<HTMLTextAreaElement>(element)) {
        return true;
    }

    if (is<HTMLInputElement>(element)
        && !element.get_attribute(HTML::AttributeNames::type).equals_ignoring_ascii_case("image"sv)) {
        return true;
    }

    return false;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-elements
JS::NonnullGCPtr<DOM::HTMLFormControlsCollection> HTMLFormElement::elements() const
{
    if (!m_elements) {
        m_elements = DOM::HTMLFormControlsCollection::create(const_cast<HTMLFormElement&>(*this), DOM::HTMLCollection::Scope::Descendants, [](Element const& element) {
            return is_form_control(element);
        });
    }
    return *m_elements;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-length
unsigned HTMLFormElement::length() const
{
    // The length IDL attribute must return the number of nodes represented by the elements collection.
    return elements()->length();
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-checkvalidity
WebIDL::ExceptionOr<bool> HTMLFormElement::check_validity()
{
    dbgln("(STUBBED) HTMLFormElement::check_validity(). Called on: {}", debug_description());
    return true;
}

// https://html.spec.whatwg.org/multipage/forms.html#dom-form-reportvalidity
WebIDL::ExceptionOr<bool> HTMLFormElement::report_validity()
{
    dbgln("(STUBBED) HTMLFormElement::report_validity(). Called on: {}", debug_description());
    return true;
}

// https://html.spec.whatwg.org/multipage/forms.html#category-submit
ErrorOr<Vector<JS::NonnullGCPtr<DOM::Element>>> HTMLFormElement::get_submittable_elements()
{
    Vector<JS::NonnullGCPtr<DOM::Element>> submittable_elements = {};
    for (size_t i = 0; i < elements()->length(); i++) {
        auto* element = elements()->item(i);
        TRY(populate_vector_with_submittable_elements_in_tree_order(*element, submittable_elements));
    }
    return submittable_elements;
}

ErrorOr<void> HTMLFormElement::populate_vector_with_submittable_elements_in_tree_order(JS::NonnullGCPtr<DOM::Element> element, Vector<JS::NonnullGCPtr<DOM::Element>>& elements)
{
    if (auto* form_associated_element = dynamic_cast<HTML::FormAssociatedElement*>(element.ptr())) {
        if (form_associated_element->is_submittable())
            TRY(elements.try_append(element));
    }

    for (size_t i = 0; i < element->children()->length(); i++) {
        auto* child = element->children()->item(i);
        TRY(populate_vector_with_submittable_elements_in_tree_order(*child, elements));
    }

    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-fs-method
StringView HTMLFormElement::method() const
{
    // The method and enctype IDL attributes must reflect the respective content attributes of the same name, limited to only known values.
    // FIXME: This should probably be `Reflect` in the IDL.
    auto method_state = method_state_from_form_element(*this);
    switch (method_state) {
    case MethodAttributeState::GET:
        return "get"sv;
    case MethodAttributeState::POST:
        return "post"sv;
    case MethodAttributeState::Dialog:
        return "dialog"sv;
    }
    VERIFY_NOT_REACHED();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-fs-method
WebIDL::ExceptionOr<void> HTMLFormElement::set_method(String const& method)
{
    // The method and enctype IDL attributes must reflect the respective content attributes of the same name, limited to only known values.
    return set_attribute(AttributeNames::method, method);
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-fs-action
String HTMLFormElement::action() const
{
    // The action IDL attribute must reflect the content attribute of the same name, except that on getting, when the
    // content attribute is missing or its value is the empty string, the element's node document's URL must be returned
    // instead.
    if (!has_attribute(AttributeNames::action))
        return MUST(document().url().to_string());

    auto action_attribute = attribute(AttributeNames::action);
    if (!action_attribute.has_value() || action_attribute->is_empty())
        return MUST(document().url().to_string());

    return action_attribute.value();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-fs-action
WebIDL::ExceptionOr<void> HTMLFormElement::set_action(String const& value)
{
    return set_attribute(AttributeNames::action, value);
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#picking-an-encoding-for-the-form
ErrorOr<String> HTMLFormElement::pick_an_encoding() const
{
    // 1. Let encoding be the document's character encoding.
    auto encoding = document().encoding_or_default();

    // 2. If the form element has an accept-charset attribute, set encoding to the return value of running these substeps:
    if (has_attribute(AttributeNames::accept_charset)) {
        // 1. Let input be the value of the form element's accept-charset attribute.
        auto input = deprecated_attribute(AttributeNames::accept_charset);

        // 2. Let candidate encoding labels be the result of splitting input on ASCII whitespace.
        auto candidate_encoding_labels = input.split_view(Infra::is_ascii_whitespace);

        // 3. Let candidate encodings be an empty list of character encodings.
        Vector<StringView> candidate_encodings;

        // 4. For each token in candidate encoding labels in turn (in the order in which they were found in input),
        //    get an encoding for the token and, if this does not result in failure, append the encoding to candidate
        //    encodings.
        for (auto const& token : candidate_encoding_labels) {
            auto candidate_encoding = TextCodec::get_standardized_encoding(token);
            if (candidate_encoding.has_value())
                TRY(candidate_encodings.try_append(candidate_encoding.value()));
        }

        // 5. If candidate encodings is empty, return UTF-8.
        if (candidate_encodings.is_empty())
            return "UTF-8"_string;

        // 6. Return the first encoding in candidate encodings.
        return String::from_utf8(candidate_encodings.first());
    }

    // 3. Return the result of getting an output encoding from encoding.
    return MUST(String::from_utf8(TextCodec::get_output_encoding(encoding)));
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#convert-to-a-list-of-name-value-pairs
static ErrorOr<Vector<URL::QueryParam>> convert_to_list_of_name_value_pairs(Vector<XHR::FormDataEntry> const& entry_list)
{
    // 1. Let list be an empty list of name-value pairs.
    Vector<URL::QueryParam> list;

    // 2. For each entry of entry list:
    for (auto const& entry : entry_list) {
        // 1. Let name be entry's name, with every occurrence of U+000D (CR) not followed by U+000A (LF), and every occurrence of U+000A (LF)
        //    not preceded by U+000D (CR), replaced by a string consisting of U+000D (CR) and U+000A (LF).
        auto name = TRY(normalize_line_breaks(entry.name));

        // 2. If entry's value is a File object, then let value be entry's value's name. Otherwise, let value be entry's value.
        String value;
        entry.value.visit(
            [&value](JS::Handle<FileAPI::File> const& file) {
                value = file->name();
            },
            [&value](String const& string) {
                value = string;
            });

        // 3. Replace every occurrence of U+000D (CR) not followed by U+000A (LF), and every occurrence of
        //    U+000A (LF) not preceded by U+000D (CR), in value, by a string consisting of U+000D (CR) and U+000A (LF).
        auto normalized_value = TRY(normalize_line_breaks(value));

        // 4. Append to list a new name-value pair whose name is name and whose value is value.
        TRY(list.try_append(URL::QueryParam { .name = move(name), .value = move(normalized_value) }));
    }

    // 3. Return list.
    return list;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#text/plain-encoding-algorithm
static ErrorOr<String> plain_text_encode(Vector<URL::QueryParam> const& pairs)
{
    // 1. Let result be the empty string.
    StringBuilder result;

    // 2. For each pair in pairs:
    for (auto const& pair : pairs) {
        // 1. Append pair's name to result.
        TRY(result.try_append(pair.name));

        // 2. Append a single U+003D EQUALS SIGN character (=) to result.
        TRY(result.try_append('='));

        // 3. Append pair's value to result.
        TRY(result.try_append(pair.value));

        // 4. Append a U+000D CARRIAGE RETURN (CR) U+000A LINE FEED (LF) character pair to result.
        TRY(result.try_append("\r\n"sv));
    }

    // 3. Return result.
    return result.to_string();
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#submit-mutate-action
ErrorOr<void> HTMLFormElement::mutate_action_url(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling)
{
    // 1. Let pairs be the result of converting to a list of name-value pairs with entry list.
    auto pairs = TRY(convert_to_list_of_name_value_pairs(entry_list));

    // 2. Let query be the result of running the application/x-www-form-urlencoded serializer with pairs and encoding.
    auto query = TRY(url_encode(pairs, encoding));

    // 3. Set parsed action's query component to query.
    parsed_action.set_query(query);

    // 4. Plan to navigate to parsed action.
    plan_to_navigate_to(move(parsed_action), Empty {}, target_navigable, history_handling);
    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#submit-body
ErrorOr<void> HTMLFormElement::submit_as_entity_body(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, EncodingTypeAttributeState encoding_type, [[maybe_unused]] String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling)
{
    // 1. Assert: method is POST.

    POSTResource::RequestContentType mime_type {};
    ByteBuffer body;

    // 2. Switch on enctype:
    switch (encoding_type) {
    case EncodingTypeAttributeState::FormUrlEncoded: {
        // -> application/x-www-form-urlencoded
        // 1. Let pairs be the result of converting to a list of name-value pairs with entry list.
        auto pairs = TRY(convert_to_list_of_name_value_pairs(entry_list));

        // 2. Let body be the result of running the application/x-www-form-urlencoded serializer with pairs and encoding.
        body = TRY(ByteBuffer::copy(TRY(url_encode(pairs, encoding)).bytes()));

        // 3. Set body to the result of encoding body.
        // NOTE: `encoding` refers to `UTF-8 encode`, which body already is encoded as because it uses AK::String.

        // 4. Let mimeType be `application/x-www-form-urlencoded`.
        mime_type = POSTResource::RequestContentType::ApplicationXWWWFormUrlencoded;
        break;
    }
    case EncodingTypeAttributeState::FormData: {
        // -> multipart/form-data
        // 1. Let body be the result of running the multipart/form-data encoding algorithm with entry list and encoding.
        auto body_and_mime_type = TRY(serialize_to_multipart_form_data(entry_list));
        body = move(body_and_mime_type.serialized_data);

        // 2. Let mimeType be the isomorphic encoding of the concatenation of "multipart/form-data; boundary=" and the multipart/form-data
        //    boundary string generated by the multipart/form-data encoding algorithm.
        mime_type = POSTResource::RequestContentType::MultipartFormData;
        return {};
    }
    case EncodingTypeAttributeState::PlainText: {
        // -> text/plain
        // 1. Let pairs be the result of converting to a list of name-value pairs with entry list.
        auto pairs = TRY(convert_to_list_of_name_value_pairs(entry_list));

        // 2. Let body be the result of running the text/plain encoding algorithm with pairs.
        body = TRY(ByteBuffer::copy(TRY(plain_text_encode(pairs)).bytes()));

        // FIXME: 3. Set body to the result of encoding body using encoding.

        // 4. Let mimeType be `text/plain`.
        mime_type = POSTResource::RequestContentType::TextPlain;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    // 3. Plan to navigate to parsed action given a POST resource whose request body is body and request content-type is mimeType.
    plan_to_navigate_to(parsed_action, POSTResource { .request_body = move(body), .request_content_type = mime_type }, target_navigable, history_handling);
    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#submit-get-action
void HTMLFormElement::get_action_url(AK::URL parsed_action, JS::NonnullGCPtr<Navigable> target_navigable, Web::HTML::HistoryHandlingBehavior history_handling)
{
    // 1. Plan to navigate to parsed action.
    // Spec Note: entry list is discarded.
    plan_to_navigate_to(move(parsed_action), Empty {}, target_navigable, history_handling);
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#submit-mailto-headers
ErrorOr<void> HTMLFormElement::mail_with_headers(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, [[maybe_unused]] String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling)
{
    // 1. Let pairs be the result of converting to a list of name-value pairs with entry list.
    auto pairs = TRY(convert_to_list_of_name_value_pairs(entry_list));

    // 2. Let headers be the result of running the application/x-www-form-urlencoded serializer with pairs and encoding.
    auto headers = TRY(url_encode(pairs, encoding));

    // 3. Replace occurrences of U+002B PLUS SIGN characters (+) in headers with the string "%20".
    TRY(headers.replace("+"sv, "%20"sv, ReplaceMode::All));

    // 4. Set parsed action's query to headers.
    parsed_action.set_query(headers);

    // 5. Plan to navigate to parsed action.
    plan_to_navigate_to(move(parsed_action), Empty {}, target_navigable, history_handling);
    return {};
}

ErrorOr<void> HTMLFormElement::mail_as_body(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, EncodingTypeAttributeState encoding_type, [[maybe_unused]] String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling)
{
    // 1. Let pairs be the result of converting to a list of name-value pairs with entry list.
    auto pairs = TRY(convert_to_list_of_name_value_pairs(entry_list));

    String body;

    // 2. Switch on enctype:
    switch (encoding_type) {
    case EncodingTypeAttributeState::PlainText: {
        // -> text/plain
        // 1. Let body be the result of running the text/plain encoding algorithm with pairs.
        body = TRY(plain_text_encode(pairs));

        // 2. Set body to the result of running UTF-8 percent-encode on body using the default encode set. [URL]
        // NOTE: body is already UTF-8 encoded due to using AK::String, so we only have to do the percent encoding.
        // NOTE: "default encode set" links to "path percent-encode-set": https://url.spec.whatwg.org/#default-encode-set
        auto percent_encoded_body = AK::URL::percent_encode(body, AK::URL::PercentEncodeSet::Path);
        body = TRY(String::from_utf8(percent_encoded_body.view()));
        break;
    }
    default:
        // -> Otherwise
        // Let body be the result of running the application/x-www-form-urlencoded serializer with pairs and encoding.
        body = TRY(url_encode(pairs, encoding));
        break;
    }

    // 3. If parsed action's query is null, then set it to the empty string.
    if (!parsed_action.query().has_value())
        parsed_action.set_query(String {});

    StringBuilder query_builder;

    query_builder.append(*parsed_action.query());

    // 4. If parsed action's query is not the empty string, then append a single U+0026 AMPERSAND character (&) to it.
    if (!parsed_action.query()->is_empty())
        TRY(query_builder.try_append('&'));

    // 5. Append "body=" to parsed action's query.
    TRY(query_builder.try_append("body="sv));

    // 6. Append body to parsed action's query.
    TRY(query_builder.try_append(body));

    parsed_action.set_query(MUST(query_builder.to_string()));

    // 7. Plan to navigate to parsed action.
    plan_to_navigate_to(move(parsed_action), Empty {}, target_navigable, history_handling);
    return {};
}

// FIXME:
static Bindings::NavigationHistoryBehavior to_navigation_history_behavior(HistoryHandlingBehavior b)
{
    switch (b) {
    case HistoryHandlingBehavior::Push:
        return Bindings::NavigationHistoryBehavior::Push;
    case HistoryHandlingBehavior::Replace:
        return Bindings::NavigationHistoryBehavior::Replace;
    default:
        return Bindings::NavigationHistoryBehavior::Auto;
    }
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#plan-to-navigate
void HTMLFormElement::plan_to_navigate_to(AK::URL url, Variant<Empty, String, POSTResource> post_resource, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling)
{
    // 1. Let referrerPolicy be the empty string.
    ReferrerPolicy::ReferrerPolicy referrer_policy = ReferrerPolicy::ReferrerPolicy::EmptyString;

    // 2. If the form element's link types include the noreferrer keyword, then set referrerPolicy to "no-referrer".
    auto rel = deprecated_attribute(HTML::AttributeNames::rel).to_lowercase();
    auto link_types = rel.view().split_view_if(Infra::is_ascii_whitespace);
    if (link_types.contains_slow("noreferrer"sv))
        referrer_policy = ReferrerPolicy::ReferrerPolicy::NoReferrer;

    // 3. If the form has a non-null planned navigation, remove it from its task queue.
    if (m_planned_navigation) {
        HTML::main_thread_event_loop().task_queue().remove_tasks_matching([this](Task const& task) {
            return &task == m_planned_navigation;
        });
    }

    // 4. Queue an element task on the DOM manipulation task source given the form element and the following steps:
    // NOTE: `this`, `actual_resource` and `target_navigable` are protected by JS::SafeFunction.
    queue_an_element_task(Task::Source::DOMManipulation, [this, url, post_resource, target_navigable, history_handling, referrer_policy]() {
        // 1. Set the form's planned navigation to null.
        m_planned_navigation = nullptr;

        // 2. Navigate targetNavigable to url using the form element's node document, with historyHandling set to historyHandling,
        //    referrerPolicy set to referrerPolicy, documentResource set to postResource, and cspNavigationType set to "form-submission".
        MUST(target_navigable->navigate(url, this->document(), post_resource, nullptr, false, to_navigation_history_behavior(history_handling), {}, {}, referrer_policy));
    });

    // 5. Set the form's planned navigation to the just-queued task.
    m_planned_navigation = HTML::main_thread_event_loop().task_queue().last_added_task();
    VERIFY(m_planned_navigation);
}

}
