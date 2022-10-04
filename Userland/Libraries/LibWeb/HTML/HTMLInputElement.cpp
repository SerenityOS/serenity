/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Adam Hodgen <ant1441@gmail.com>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

HTMLInputElement::HTMLInputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_value(String::empty())
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLInputElement"));

    activation_behavior = [this](auto&) {
        // The activation behavior for input elements are these steps:

        // FIXME: 1. If this element is not mutable and is not in the Checkbox state and is not in the Radio state, then return.

        // 2. Run this element's input activation behavior, if any, and do nothing otherwise.
        run_input_activation_behavior();
    };
}

HTMLInputElement::~HTMLInputElement() = default;

void HTMLInputElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_text_node.ptr());
    visitor.visit(m_legacy_pre_activation_behavior_checked_element_in_group.ptr());
    visitor.visit(m_selected_files);
}

RefPtr<Layout::Node> HTMLInputElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (type_state() == TypeAttributeState::Hidden)
        return nullptr;

    if (type_state() == TypeAttributeState::SubmitButton || type_state() == TypeAttributeState::Button || type_state() == TypeAttributeState::ResetButton || type_state() == TypeAttributeState::FileUpload)
        return adopt_ref(*new Layout::ButtonBox(document(), *this, move(style)));

    if (type_state() == TypeAttributeState::Checkbox)
        return adopt_ref(*new Layout::CheckBox(document(), *this, move(style)));

    if (type_state() == TypeAttributeState::RadioButton)
        return adopt_ref(*new Layout::RadioButton(document(), *this, move(style)));

    auto layout_node = adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

void HTMLInputElement::set_checked(bool checked, ChangeSource change_source)
{
    if (m_checked == checked)
        return;

    // The dirty checkedness flag must be initially set to false when the element is created,
    // and must be set to true whenever the user interacts with the control in a way that changes the checkedness.
    if (change_source == ChangeSource::User)
        m_dirty_checkedness = true;

    m_checked = checked;
    set_needs_style_update(true);
}

void HTMLInputElement::set_checked_binding(bool checked)
{
    if (type_state() == TypeAttributeState::RadioButton) {
        if (checked)
            set_checked_within_group();
        else
            set_checked(false, ChangeSource::Programmatic);
    } else {
        set_checked(checked, ChangeSource::Programmatic);
    }
}

// https://html.spec.whatwg.org/multipage/input.html#dom-input-files
JS::GCPtr<FileAPI::FileList> HTMLInputElement::files()
{
    // On getting, if the IDL attribute applies, it must return a FileList object that represents the current selected files.
    //  The same object must be returned until the list of selected files changes.
    // If the IDL attribute does not apply, then it must instead return null.
    if (m_type != TypeAttributeState::FileUpload)
        return nullptr;

    if (!m_selected_files)
        m_selected_files = FileAPI::FileList::create(realm(), {});
    return m_selected_files;
}

// https://html.spec.whatwg.org/multipage/input.html#dom-input-files
void HTMLInputElement::set_files(JS::GCPtr<FileAPI::FileList> files)
{
    // 1. If the IDL attribute does not apply or the given value is null, then return.
    if (m_type != TypeAttributeState::FileUpload || files == nullptr)
        return;

    // 2. Replace the element's selected files with the given value.
    m_selected_files = files;
}

// https://html.spec.whatwg.org/multipage/input.html#update-the-file-selection
void HTMLInputElement::update_the_file_selection(JS::NonnullGCPtr<FileAPI::FileList> files)
{
    // 1. Queue an element task on the user interaction task source given element and the following steps:
    queue_an_element_task(Task::Source::UserInteraction, [this, files]() mutable {
        // 1. Update element's selected files so that it represents the user's selection.
        this->set_files(files.ptr());

        // 2. Fire an event named input at the input element, with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(this->realm(), EventNames::input, { .bubbles = true, .composed = true });
        this->dispatch_event(*input_event);

        // 3. Fire an event named change at the input element, with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(this->realm(), EventNames::change, { .bubbles = true });
        this->dispatch_event(*change_event);
    });
}

// https://html.spec.whatwg.org/multipage/input.html#show-the-picker,-if-applicable
static void show_the_picker_if_applicable(HTMLInputElement& element)
{
    // To show the picker, if applicable for an input element element:

    // 1. If element's relevant global object does not have transient activation, then return.
    auto& global_object = relevant_global_object(element);
    if (!is<HTML::Window>(global_object) || !static_cast<HTML::Window&>(global_object).has_transient_activation())
        return;

    // FIXME: 2. If element is not mutable, then return.

    // 3. If element's type attribute is in the File Upload state, then run these steps in parallel:
    if (element.type_state() == HTMLInputElement::TypeAttributeState::FileUpload) {
        // NOTE: These steps cannot be fully implemented here, and must be done in the PageClient when the response comes back from the PageHost

        // 1. Optionally, wait until any prior execution of this algorithm has terminated.
        // 2. Display a prompt to the user requesting that the user specify some files.
        //    If the multiple attribute is not set on element, there must be no more than one file selected; otherwise, any number may be selected.
        //    Files can be from the filesystem or created on the fly, e.g., a picture taken from a camera connected to the user's device.
        // 3. Wait for the user to have made their selection.
        // 4. If the user dismissed the prompt without changing their selection,
        //    then queue an element task on the user interaction task source given element to fire an event named cancel at element,
        //    with the bubbles attribute initialized to true.
        // 5. Otherwise, update the file selection for element.

        bool const multiple = element.has_attribute(HTML::AttributeNames::multiple);
        auto weak_element = element.make_weak_ptr<DOM::EventTarget>();

        // FIXME: Pass along accept attribute information https://html.spec.whatwg.org/multipage/input.html#attr-input-accept
        //    The accept attribute may be specified to provide user agents with a hint of what file types will be accepted.
        element.document().browsing_context()->top_level_browsing_context().page()->client().page_did_request_file_picker(weak_element, multiple);
        return;
    }

    // FIXME: show "any relevant user interface" for other type attribute states "in the way [the user agent] normally would"

    // 4. Otherwise, the user agent should show any relevant user interface for selecting a value for element,
    //    in the way it normally would when the user interacts with the control. (If no such UI applies to element, then this step does nothing.)
    //    If such a user interface is shown, it must respect the requirements stated in the relevant parts of the specification for how element
    //    behaves given its type attribute state. (For example, various sections describe restrictions on the resulting value string.)
    //    This step can have side effects, such as closing other pickers that were previously shown by this algorithm.
    //    (If this closes a file selection picker, then per the above that will lead to firing either input and change events, or a cancel event.)
}

// https://html.spec.whatwg.org/multipage/input.html#dom-input-showpicker
WebIDL::ExceptionOr<void> HTMLInputElement::show_picker()
{
    // The showPicker() method steps are:

    // FIXME: 1. If this is not mutable, then throw an "InvalidStateError" DOMException.

    // 2. If this's relevant settings object's origin is not same origin with this's relevant settings object's top-level origin,
    // and this's type attribute is not in the File Upload state or Color state, then throw a "SecurityError" DOMException.
    // NOTE: File and Color inputs are exempted from this check for historical reason: their input activation behavior also shows their pickers,
    //       and has never been guarded by an origin check.
    if (!relevant_settings_object(*this).origin().is_same_origin(relevant_settings_object(*this).top_level_origin)
        && m_type != TypeAttributeState::FileUpload && m_type != TypeAttributeState::Color) {
        return WebIDL::SecurityError::create(realm(), "Cross origin pickers are not allowed"sv);
    }

    // 3. If this's relevant global object does not have transient activation, then throw a "NotAllowedError" DOMException.
    // FIXME: The global object we get here should probably not need casted to Window to check for transient activation
    auto& global_object = relevant_global_object(*this);
    if (!is<HTML::Window>(global_object) || !static_cast<HTML::Window&>(global_object).has_transient_activation()) {
        return WebIDL::NotAllowedError::create(realm(), "Too long since user activation to show picker"sv);
    }

    // 4. Show the picker, if applicable, for this.
    show_the_picker_if_applicable(*this);
    return {};
}

// https://html.spec.whatwg.org/multipage/input.html#input-activation-behavior
void HTMLInputElement::run_input_activation_behavior()
{
    if (type_state() == TypeAttributeState::Checkbox || type_state() == TypeAttributeState::RadioButton) {
        // 1. If the element is not connected, then return.
        if (!is_connected())
            return;

        // 2. Fire an event named input at the element with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(realm(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(*input_event);

        // 3. Fire an event named change at the element with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(realm(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(*change_event);
    } else if (type_state() == TypeAttributeState::SubmitButton) {
        JS::GCPtr<HTMLFormElement> form;
        // 1. If the element does not have a form owner, then return.
        if (!(form = this->form()))
            return;

        // 2. If the element's node document is not fully active, then return.
        if (!document().is_fully_active())
            return;

        // 3. Submit the form owner from the element.
        form->submit_form(this);
    } else if (type_state() == TypeAttributeState::FileUpload) {
        show_the_picker_if_applicable(*this);
    } else {
        dispatch_event(*DOM::Event::create(realm(), EventNames::change));
    }
}

void HTMLInputElement::did_edit_text_node(Badge<BrowsingContext>)
{
    // An input element's dirty value flag must be set to true whenever the user interacts with the control in a way that changes the value.
    m_value = value_sanitization_algorithm(m_text_node->data());
    m_dirty_value = true;

    // NOTE: This is a bit ad-hoc, but basically implements part of "4.10.5.5 Common event behaviors"
    //       https://html.spec.whatwg.org/multipage/input.html#common-input-element-events
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto input_event = DOM::Event::create(realm(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(*input_event);

        // FIXME: This should only fire when the input is "committed", whatever that means.
        auto change_event = DOM::Event::create(realm(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(*change_event);
    });
}

String HTMLInputElement::value() const
{
    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-filename
    if (type_state() == TypeAttributeState::FileUpload) {
        // NOTE: This "fakepath" requirement is a sad accident of history. See the example in the File Upload state section for more information.
        // NOTE: Since path components are not permitted in filenames in the list of selected files, the "\fakepath\" cannot be mistaken for a path component.
        if (m_selected_files && m_selected_files->item(0))
            return String::formatted("C:\\fakepath\\{}", m_selected_files->item(0)->name());
        return "C:\\fakepath\\"sv;
    }

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-value
    // Return the current value of the element.
    return m_value;
}

WebIDL::ExceptionOr<void> HTMLInputElement::set_value(String value)
{
    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-filename
    if (type_state() == TypeAttributeState::FileUpload) {
        // On setting, if the new value is the empty string, empty the list of selected files; otherwise, throw an "InvalidStateError" DOMException.
        if (value != String::empty())
            return WebIDL::InvalidStateError::create(realm(), "Setting value of input type file to non-empty string"sv);
        m_selected_files = nullptr;
        return {};
    }

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-value
    // 1. Let oldValue be the element's value.
    auto old_value = move(m_value);

    // 2. Set the element's value to the new value.
    // NOTE: This is done as part of step 4 below.

    // 3. Set the element's dirty value flag to true.
    m_dirty_value = true;

    // 4. Invoke the value sanitization algorithm, if the element's type attribute's current state defines one.
    m_value = value_sanitization_algorithm(move(value));

    // 5. If the element's value (after applying the value sanitization algorithm) is different from oldValue,
    //    and the element has a text entry cursor position, move the text entry cursor position to the end of the
    //    text control, unselecting any selected text and resetting the selection direction to "none".
    if (m_text_node && (m_value != old_value))
        m_text_node->set_data(m_value);

    return {};
}

void HTMLInputElement::create_shadow_tree_if_needed()
{
    if (shadow_root())
        return;

    // FIXME: This could be better factored. Everything except the below types becomes a text input.
    switch (type_state()) {
    case TypeAttributeState::RadioButton:
    case TypeAttributeState::Checkbox:
    case TypeAttributeState::Button:
    case TypeAttributeState::SubmitButton:
    case TypeAttributeState::ResetButton:
    case TypeAttributeState::ImageButton:
        return;
    default:
        break;
    }

    auto* shadow_root = heap().allocate<DOM::ShadowRoot>(realm(), document(), *this);
    auto initial_value = m_value;
    if (initial_value.is_null())
        initial_value = String::empty();
    auto element = document().create_element(HTML::TagNames::div).release_value();
    element->set_attribute(HTML::AttributeNames::style, "white-space: pre; padding-top: 1px; padding-bottom: 1px; padding-left: 2px; padding-right: 2px");
    m_text_node = heap().allocate<DOM::Text>(realm(), document(), initial_value);
    m_text_node->set_always_editable(m_type != TypeAttributeState::FileUpload);
    m_text_node->set_owner_input_element({}, *this);
    element->append_child(*m_text_node);
    shadow_root->append_child(move(element));
    set_shadow_root(move(shadow_root));
}

void HTMLInputElement::did_receive_focus()
{
    auto* browsing_context = document().browsing_context();
    if (!browsing_context)
        return;
    if (!m_text_node)
        return;
    browsing_context->set_cursor_position(DOM::Position { *m_text_node, 0 });
}

void HTMLInputElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is added, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to true
        if (!m_dirty_checkedness)
            set_checked(true, ChangeSource::Programmatic);
    } else if (name == HTML::AttributeNames::type) {
        m_type = parse_type_attribute(value);
    } else if (name == HTML::AttributeNames::value) {
        if (!m_dirty_value)
            m_value = value_sanitization_algorithm(value);
    }
}

HTMLInputElement::TypeAttributeState HTMLInputElement::parse_type_attribute(StringView type)
{
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    if (type.equals_ignoring_case(#keyword##sv))              \
        return HTMLInputElement::TypeAttributeState::state;
    ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE

    // The missing value default and the invalid value default are the Text state.
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:missing-value-default
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:invalid-value-default
    return HTMLInputElement::TypeAttributeState::Text;
}

void HTMLInputElement::did_remove_attribute(FlyString const& name)
{
    HTMLElement::did_remove_attribute(name);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is removed, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to false.
        if (!m_dirty_checkedness)
            set_checked(false, ChangeSource::Programmatic);
    } else if (name == HTML::AttributeNames::value) {
        if (!m_dirty_value)
            m_value = String::empty();
    }
}

String HTMLInputElement::type() const
{
    switch (m_type) {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    case TypeAttributeState::state:                           \
        return #keyword##sv;
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    }

    VERIFY_NOT_REACHED();
}

void HTMLInputElement::set_type(String const& type)
{
    set_attribute(HTML::AttributeNames::type, type);
}

// https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
String HTMLInputElement::value_sanitization_algorithm(String value) const
{
    if (type_state() == HTMLInputElement::TypeAttributeState::Text || type_state() == HTMLInputElement::TypeAttributeState::Search || type_state() == HTMLInputElement::TypeAttributeState::Telephone || type_state() == HTMLInputElement::TypeAttributeState::Password) {
        // Strip newlines from the value.
        if (value.contains('\r') || value.contains('\n')) {
            StringBuilder builder;
            for (auto c : value) {
                if (!(c == '\r' || c == '\n'))
                    builder.append(c);
            }
            return builder.to_string();
        }
    } else if (type_state() == HTMLInputElement::TypeAttributeState::URL) {
        // Strip newlines from the value, then strip leading and trailing ASCII whitespace from the value.
        if (value.contains('\r') || value.contains('\n')) {
            StringBuilder builder;
            for (auto c : value) {
                if (!(c == '\r' || c == '\n'))
                    builder.append(c);
            }
            return builder.string_view().trim(Infra::ASCII_WHITESPACE);
        }
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Number) {
        // If the value of the element is not a valid floating-point number, then set it to the empty string instead.
        char* end_ptr;
        auto val = strtod(value.characters(), &end_ptr);
        if (!isfinite(val) || *end_ptr)
            return "";
    }

    // FIXME: Implement remaining value sanitation algorithms
    return value;
}

void HTMLInputElement::form_associated_element_was_inserted()
{
    create_shadow_tree_if_needed();
}

void HTMLInputElement::set_checked_within_group()
{
    if (checked())
        return;

    set_checked(true, ChangeSource::User);
    String name = this->name();

    document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
        if (element.checked() && &element != this && element.name() == name)
            element.set_checked(false, ChangeSource::User);
        return IterationDecision::Continue;
    });
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-pre-activation-behavior
void HTMLInputElement::legacy_pre_activation_behavior()
{
    m_before_legacy_pre_activation_behavior_checked = checked();

    // 1. If this element's type attribute is in the Checkbox state, then set
    // this element's checkedness to its opposite value (i.e. true if it is
    // false, false if it is true) and set this element's indeterminate IDL
    // attribute to false.
    // FIXME: Set indeterminate to false when that exists.
    if (type_state() == TypeAttributeState::Checkbox) {
        set_checked(!checked(), ChangeSource::User);
    }

    // 2. If this element's type attribute is in the Radio Button state, then
    // get a reference to the element in this element's radio button group that
    // has its checkedness set to true, if any, and then set this element's
    // checkedness to true.
    if (type_state() == TypeAttributeState::RadioButton) {
        String name = this->name();

        document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
            if (element.checked() && element.name() == name) {
                m_legacy_pre_activation_behavior_checked_element_in_group = &element;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        set_checked_within_group();
    }
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-canceled-activation-behavior
void HTMLInputElement::legacy_cancelled_activation_behavior()
{
    // 1. If the element's type attribute is in the Checkbox state, then set the
    // element's checkedness and the element's indeterminate IDL attribute back
    // to the values they had before the legacy-pre-activation behavior was run.
    if (type_state() == TypeAttributeState::Checkbox) {
        set_checked(m_before_legacy_pre_activation_behavior_checked, ChangeSource::Programmatic);
    }

    // 2. If this element 's type attribute is in the Radio Button state, then
    // if the element to which a reference was obtained in the
    // legacy-pre-activation behavior, if any, is still in what is now this
    // element' s radio button group, if it still has one, and if so, setting
    // that element 's checkedness to true; or else, if there was no such
    // element, or that element is no longer in this element' s radio button
    // group, or if this element no longer has a radio button group, setting
    // this element's checkedness to false.
    if (type_state() == TypeAttributeState::RadioButton) {
        String name = this->name();
        bool did_reselect_previous_element = false;
        if (m_legacy_pre_activation_behavior_checked_element_in_group) {
            auto& element_in_group = *m_legacy_pre_activation_behavior_checked_element_in_group;
            if (name == element_in_group.name()) {
                element_in_group.set_checked_within_group();
                did_reselect_previous_element = true;
            }

            m_legacy_pre_activation_behavior_checked_element_in_group = nullptr;
        }

        if (!did_reselect_previous_element)
            set_checked(false, ChangeSource::User);
    }
}

void HTMLInputElement::legacy_cancelled_activation_behavior_was_not_called()
{
    m_legacy_pre_activation_behavior_checked_element_in_group = nullptr;
}

}
