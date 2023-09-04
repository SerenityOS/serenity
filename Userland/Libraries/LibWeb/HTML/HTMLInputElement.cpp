/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Adam Hodgen <ant1441@gmail.com>
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLDivElement.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

HTMLInputElement::HTMLInputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_value(DeprecatedString::empty())
{
    activation_behavior = [this](auto&) {
        // The activation behavior for input elements are these steps:

        // FIXME: 1. If this element is not mutable and is not in the Checkbox state and is not in the Radio state, then return.

        // 2. Run this element's input activation behavior, if any, and do nothing otherwise.
        run_input_activation_behavior().release_value_but_fixme_should_propagate_errors();
    };
}

HTMLInputElement::~HTMLInputElement() = default;

void HTMLInputElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLInputElementPrototype>(realm, "HTMLInputElement"));
}

void HTMLInputElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_inner_text_element);
    visitor.visit(m_text_node);
    visitor.visit(m_placeholder_element);
    visitor.visit(m_placeholder_text_node);
    visitor.visit(m_legacy_pre_activation_behavior_checked_element_in_group.ptr());
    visitor.visit(m_selected_files);
}

JS::GCPtr<Layout::Node> HTMLInputElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (type_state() == TypeAttributeState::Hidden)
        return nullptr;

    if (type_state() == TypeAttributeState::SubmitButton || type_state() == TypeAttributeState::Button || type_state() == TypeAttributeState::ResetButton || type_state() == TypeAttributeState::FileUpload)
        return heap().allocate_without_realm<Layout::ButtonBox>(document(), *this, move(style));

    if (type_state() == TypeAttributeState::Checkbox)
        return heap().allocate_without_realm<Layout::CheckBox>(document(), *this, move(style));

    if (type_state() == TypeAttributeState::RadioButton)
        return heap().allocate_without_realm<Layout::RadioButton>(document(), *this, move(style));

    // AD-HOC: We rewrite `display: inline` to `display: inline-block`.
    //         This is required for the internal shadow tree to work correctly in layout.
    if (style->display().is_inline_outside() && style->display().is_flow_inside())
        style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::InlineBlock)));

    return Element::create_layout_node_for_display_type(document(), style->display(), style, this);
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

    // This element's :checked pseudo-class could be used in a sibling's sibling-selector,
    // so we need to invalidate the style of all siblings.
    if (parent()) {
        parent()->for_each_child([&](auto& child) {
            child.invalidate_style();
        });
    }
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

// https://html.spec.whatwg.org/multipage/input.html#dom-input-indeterminate
void HTMLInputElement::set_indeterminate(bool value)
{
    // On setting, it must be set to the new value. It has no effect except for changing the appearance of checkbox controls.
    m_indeterminate = value;
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
    queue_an_element_task(Task::Source::UserInteraction, [this, files] {
        // 1. Update element's selected files so that it represents the user's selection.
        this->set_files(files.ptr());

        // 2. Fire an event named input at the input element, with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(this->realm(), EventNames::input, { .bubbles = true, .composed = true });
        this->dispatch_event(input_event);

        // 3. Fire an event named change at the input element, with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(this->realm(), EventNames::change, { .bubbles = true });
        this->dispatch_event(change_event);
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

    // 2. If element is not mutable, then return.
    if (!element.is_mutable())
        return;

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
        element.document().browsing_context()->top_level_browsing_context()->page()->client().page_did_request_file_picker(weak_element, multiple);
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

    // 1. If this is not mutable, then throw an "InvalidStateError" DOMException.
    if (!m_is_mutable)
        return WebIDL::InvalidStateError::create(realm(), "Element is not mutable"_fly_string);

    // 2. If this's relevant settings object's origin is not same origin with this's relevant settings object's top-level origin,
    // and this's type attribute is not in the File Upload state or Color state, then throw a "SecurityError" DOMException.
    // NOTE: File and Color inputs are exempted from this check for historical reason: their input activation behavior also shows their pickers,
    //       and has never been guarded by an origin check.
    if (!relevant_settings_object(*this).origin().is_same_origin(relevant_settings_object(*this).top_level_origin)
        && m_type != TypeAttributeState::FileUpload && m_type != TypeAttributeState::Color) {
        return WebIDL::SecurityError::create(realm(), "Cross origin pickers are not allowed"_fly_string);
    }

    // 3. If this's relevant global object does not have transient activation, then throw a "NotAllowedError" DOMException.
    // FIXME: The global object we get here should probably not need casted to Window to check for transient activation
    auto& global_object = relevant_global_object(*this);
    if (!is<HTML::Window>(global_object) || !static_cast<HTML::Window&>(global_object).has_transient_activation()) {
        return WebIDL::NotAllowedError::create(realm(), "Too long since user activation to show picker"_fly_string);
    }

    // 4. Show the picker, if applicable, for this.
    show_the_picker_if_applicable(*this);
    return {};
}

// https://html.spec.whatwg.org/multipage/input.html#input-activation-behavior
WebIDL::ExceptionOr<void> HTMLInputElement::run_input_activation_behavior()
{
    if (type_state() == TypeAttributeState::Checkbox || type_state() == TypeAttributeState::RadioButton) {
        // 1. If the element is not connected, then return.
        if (!is_connected())
            return {};

        // 2. Fire an event named input at the element with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(realm(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(input_event);

        // 3. Fire an event named change at the element with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(realm(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(*change_event);
    } else if (type_state() == TypeAttributeState::SubmitButton) {
        JS::GCPtr<HTMLFormElement> form;
        // 1. If the element does not have a form owner, then return.
        if (!(form = this->form()))
            return {};

        // 2. If the element's node document is not fully active, then return.
        if (!document().is_fully_active())
            return {};

        // 3. Submit the form owner from the element.
        TRY(form->submit_form(*this));
    } else if (type_state() == TypeAttributeState::FileUpload) {
        show_the_picker_if_applicable(*this);
    } else {
        dispatch_event(DOM::Event::create(realm(), EventNames::change));
    }

    return {};
}

void HTMLInputElement::did_edit_text_node(Badge<BrowsingContext>)
{
    // An input element's dirty value flag must be set to true whenever the user interacts with the control in a way that changes the value.
    m_value = value_sanitization_algorithm(m_text_node->data());
    m_dirty_value = true;

    update_placeholder_visibility();

    // NOTE: This is a bit ad-hoc, but basically implements part of "4.10.5.5 Common event behaviors"
    //       https://html.spec.whatwg.org/multipage/input.html#common-input-element-events
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto input_event = DOM::Event::create(realm(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(*input_event);
    });
}

DeprecatedString HTMLInputElement::value() const
{
    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-filename
    if (type_state() == TypeAttributeState::FileUpload) {
        // NOTE: This "fakepath" requirement is a sad accident of history. See the example in the File Upload state section for more information.
        // NOTE: Since path components are not permitted in filenames in the list of selected files, the "\fakepath\" cannot be mistaken for a path component.
        // On getting, return the string "C:\fakepath\" followed by the name of the first file in the list of selected files, if any, or the empty string if the list is empty.
        if (m_selected_files && m_selected_files->item(0))
            return DeprecatedString::formatted("C:\\fakepath\\{}", m_selected_files->item(0)->name());
        return DeprecatedString::empty();
    }

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-default-on
    if (type_state() == TypeAttributeState::Checkbox || type_state() == TypeAttributeState::RadioButton) {
        // On getting, if the element has a value content attribute, return that attribute's value; otherwise, return the string "on".
        return has_attribute(AttributeNames::value) ? get_attribute(AttributeNames::value) : "on";
    }

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-default
    if (type_state() == TypeAttributeState::Hidden
        || type_state() == TypeAttributeState::SubmitButton
        || type_state() == TypeAttributeState::ImageButton
        || type_state() == TypeAttributeState::ResetButton
        || type_state() == TypeAttributeState::Button) {
        // On getting, if the element has a value content attribute, return that attribute's value; otherwise, return the empty string.
        return has_attribute(AttributeNames::value) ? get_attribute(AttributeNames::value) : DeprecatedString::empty();
    }

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-value
    // Return the current value of the element.
    return m_value;
}

WebIDL::ExceptionOr<void> HTMLInputElement::set_value(String const& value)
{
    // https://html.spec.whatwg.org/multipage/input.html#dom-input-value-filename
    if (type_state() == TypeAttributeState::FileUpload) {
        // On setting, if the new value is the empty string, empty the list of selected files; otherwise, throw an "InvalidStateError" DOMException.
        if (!value.is_empty())
            return WebIDL::InvalidStateError::create(realm(), "Setting value of input type file to non-empty string"_fly_string);
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
    m_value = value_sanitization_algorithm(value.to_deprecated_string());

    // 5. If the element's value (after applying the value sanitization algorithm) is different from oldValue,
    //    and the element has a text entry cursor position, move the text entry cursor position to the end of the
    //    text control, unselecting any selected text and resetting the selection direction to "none".
    if (m_text_node && (m_value != old_value)) {
        m_text_node->set_data(m_value);
        update_placeholder_visibility();
    }

    return {};
}

void HTMLInputElement::update_placeholder_visibility()
{
    if (!m_placeholder_element)
        return;
    auto placeholder_text = this->placeholder_value();
    if (placeholder_text.has_value()) {
        MUST(m_placeholder_element->style_for_bindings()->set_property(CSS::PropertyID::Display, "block"sv));
    } else {
        MUST(m_placeholder_element->style_for_bindings()->set_property(CSS::PropertyID::Display, "none"sv));
    }
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:attr-input-readonly-3
static bool is_allowed_to_be_readonly(HTML::HTMLInputElement::TypeAttributeState state)
{
    switch (state) {
    case HTML::HTMLInputElement::TypeAttributeState::Text:
    case HTML::HTMLInputElement::TypeAttributeState::Search:
    case HTML::HTMLInputElement::TypeAttributeState::Telephone:
    case HTML::HTMLInputElement::TypeAttributeState::URL:
    case HTML::HTMLInputElement::TypeAttributeState::Email:
    case HTML::HTMLInputElement::TypeAttributeState::Password:
    case HTML::HTMLInputElement::TypeAttributeState::Date:
    case HTML::HTMLInputElement::TypeAttributeState::Month:
    case HTML::HTMLInputElement::TypeAttributeState::Week:
    case HTML::HTMLInputElement::TypeAttributeState::Time:
    case HTML::HTMLInputElement::TypeAttributeState::LocalDateAndTime:
    case HTML::HTMLInputElement::TypeAttributeState::Number:
        return true;
    default:
        return false;
    }
}

// https://html.spec.whatwg.org/multipage/input.html#attr-input-readonly
void HTMLInputElement::handle_readonly_attribute(DeprecatedFlyString const& value)
{
    // The readonly attribute is a boolean attribute that controls whether or not the user can edit the form control. When specified, the element is not mutable.
    m_is_mutable = !(!value.is_null() && is_allowed_to_be_readonly(m_type));

    if (m_text_node)
        m_text_node->set_always_editable(m_is_mutable);
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:attr-input-placeholder-3
static bool is_allowed_to_have_placeholder(HTML::HTMLInputElement::TypeAttributeState state)
{
    switch (state) {
    case HTML::HTMLInputElement::TypeAttributeState::Text:
    case HTML::HTMLInputElement::TypeAttributeState::Search:
    case HTML::HTMLInputElement::TypeAttributeState::URL:
    case HTML::HTMLInputElement::TypeAttributeState::Telephone:
    case HTML::HTMLInputElement::TypeAttributeState::Email:
    case HTML::HTMLInputElement::TypeAttributeState::Password:
    case HTML::HTMLInputElement::TypeAttributeState::Number:
        return true;
    default:
        return false;
    }
}

// https://html.spec.whatwg.org/multipage/input.html#attr-input-placeholder
Optional<DeprecatedString> HTMLInputElement::placeholder_value() const
{
    if (!m_text_node || !m_text_node->data().is_empty())
        return {};
    if (!is_allowed_to_have_placeholder(type_state()))
        return {};
    if (!has_attribute(HTML::AttributeNames::placeholder))
        return {};

    auto placeholder = deprecated_attribute(HTML::AttributeNames::placeholder);

    if (placeholder.contains('\r') || placeholder.contains('\n')) {
        StringBuilder builder;
        for (auto ch : placeholder) {
            if (ch != '\r' && ch != '\n')
                builder.append(ch);
        }
        placeholder = builder.to_deprecated_string();
    }

    return placeholder;
}

class PlaceholderElement final : public HTMLDivElement {
    JS_CELL(PlaceholderElement, HTMLDivElement);

public:
    PlaceholderElement(DOM::Document& document)
        : HTMLDivElement(document, DOM::QualifiedName { HTML::TagNames::div, ""sv, Namespace::HTML })
    {
    }
    virtual Optional<CSS::Selector::PseudoElement> pseudo_element() const override { return CSS::Selector::PseudoElement::Placeholder; }
};

void HTMLInputElement::create_shadow_tree_if_needed()
{
    if (shadow_root_internal())
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

    auto shadow_root = heap().allocate<DOM::ShadowRoot>(realm(), document(), *this, Bindings::ShadowRootMode::Closed);
    auto initial_value = m_value;
    if (initial_value.is_null())
        initial_value = DeprecatedString::empty();
    auto element = DOM::create_element(document(), HTML::TagNames::div, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(element->set_attribute(HTML::AttributeNames::style, R"~~~(
        display: flex;
        height: 100%;
        align-items: center;
        white-space: pre;
        border: none;
        padding: 1px 2px;
)~~~"));

    m_placeholder_element = heap().allocate<PlaceholderElement>(realm(), document());
    MUST(m_placeholder_element->style_for_bindings()->set_property(CSS::PropertyID::Height, "1lh"sv));

    m_placeholder_text_node = heap().allocate<DOM::Text>(realm(), document(), MUST(String::from_deprecated_string(initial_value)));
    m_placeholder_text_node->set_data(deprecated_attribute(HTML::AttributeNames::placeholder));
    m_placeholder_text_node->set_editable_text_node_owner(Badge<HTMLInputElement> {}, *this);
    MUST(m_placeholder_element->append_child(*m_placeholder_text_node));
    MUST(element->append_child(*m_placeholder_element));

    m_inner_text_element = DOM::create_element(document(), HTML::TagNames::div, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(m_inner_text_element->style_for_bindings()->set_property(CSS::PropertyID::Height, "1lh"sv));

    m_text_node = heap().allocate<DOM::Text>(realm(), document(), MUST(String::from_deprecated_string(initial_value)));
    if (m_type == TypeAttributeState::FileUpload) {
        // NOTE: file upload state is mutable, but we don't allow the text node to be modifed
        m_text_node->set_always_editable(false);
    } else {
        handle_readonly_attribute(deprecated_attribute(HTML::AttributeNames::readonly));
    }

    m_text_node->set_editable_text_node_owner(Badge<HTMLInputElement> {}, *this);

    if (m_type == TypeAttributeState::Password)
        m_text_node->set_is_password_input({}, true);

    MUST(m_inner_text_element->append_child(*m_text_node));
    MUST(element->append_child(*m_inner_text_element));
    MUST(shadow_root->append_child(element));
    set_shadow_root(shadow_root);
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

void HTMLInputElement::did_lose_focus()
{
    // The change event fires when the value is committed, if that makes sense for the control,
    // or else when the control loses focus
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto change_event = DOM::Event::create(realm(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(change_event);
    });
}

void HTMLInputElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);
    if (name == HTML::AttributeNames::checked) {
        if (value.is_null()) {
            // When the checked content attribute is removed, if the control does not have dirty checkedness,
            // the user agent must set the checkedness of the element to false.
            if (!m_dirty_checkedness)
                set_checked(false, ChangeSource::Programmatic);
        } else {
            // When the checked content attribute is added, if the control does not have dirty checkedness,
            // the user agent must set the checkedness of the element to true
            if (!m_dirty_checkedness)
                set_checked(true, ChangeSource::Programmatic);
        }
    } else if (name == HTML::AttributeNames::type) {
        m_type = parse_type_attribute(value);
    } else if (name == HTML::AttributeNames::value) {
        if (value.is_null()) {
            if (!m_dirty_value) {
                m_value = DeprecatedString::empty();
                update_placeholder_visibility();
            }
        } else {
            if (!m_dirty_value) {
                m_value = value_sanitization_algorithm(value);
                update_placeholder_visibility();
            }
        }
    } else if (name == HTML::AttributeNames::placeholder) {
        if (m_placeholder_text_node)
            m_placeholder_text_node->set_data(value);
    } else if (name == HTML::AttributeNames::readonly) {
        handle_readonly_attribute(value);
    }
}

HTMLInputElement::TypeAttributeState HTMLInputElement::parse_type_attribute(StringView type)
{
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    if (type.equals_ignoring_ascii_case(#keyword##sv))        \
        return HTMLInputElement::TypeAttributeState::state;
    ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE

    // The missing value default and the invalid value default are the Text state.
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:missing-value-default
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:invalid-value-default
    return HTMLInputElement::TypeAttributeState::Text;
}

StringView HTMLInputElement::type() const
{
    // FIXME: This should probably be `Reflect` in the IDL.
    switch (m_type) {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    case TypeAttributeState::state:                           \
        return #keyword##sv;
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    }

    VERIFY_NOT_REACHED();
}

WebIDL::ExceptionOr<void> HTMLInputElement::set_type(String const& type)
{
    return set_attribute(HTML::AttributeNames::type, type);
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-simple-colour
static bool is_valid_simple_color(DeprecatedString const& value)
{
    // if it is exactly seven characters long,
    if (value.length() != 7)
        return false;
    // and the first character is a U+0023 NUMBER SIGN character (#),
    if (!value.starts_with('#'))
        return false;
    // and the remaining six characters are all ASCII hex digits
    for (size_t i = 1; i < value.length(); i++)
        if (!is_ascii_hex_digit(value[i]))
            return false;

    return true;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-time-string
static bool is_valid_time_string(DeprecatedString const& value)
{
    // A string is a valid time string representing an hour hour, a minute minute, and a second second if it consists of the following components in the given order:

    // 1. Two ASCII digits, representing hour, in the range 0 ≤ hour ≤ 23
    // 2. A U+003A COLON character (:)
    // 3. Two ASCII digits, representing minute, in the range 0 ≤ minute ≤ 59
    // 4. If second is nonzero, or optionally if second is zero:
    // 1. A U+003A COLON character (:)
    // 2. Two ASCII digits, representing the integer part of second, in the range 0 ≤ s ≤ 59
    // 3. If second is not an integer, or optionally if second is an integer:
    // 1. A U+002E FULL STOP character (.)
    // 2. One, two, or three ASCII digits, representing the fractional part of second
    auto parts = value.split(':');
    if (parts.size() != 2 || parts.size() != 3)
        return false;
    if (parts[0].length() != 2)
        return false;
    auto hour = (parse_ascii_digit(parts[0][0]) * 10) + parse_ascii_digit(parts[0][1]);
    if (hour > 23)
        return false;
    if (parts[1].length() != 2)
        return false;
    auto minute = (parse_ascii_digit(parts[1][0]) * 10) + parse_ascii_digit(parts[1][1]);
    if (minute > 59)
        return false;
    if (parts.size() == 2)
        return true;

    if (parts[2].length() < 2)
        return false;
    auto second = (parse_ascii_digit(parts[2][0]) * 10) + parse_ascii_digit(parts[2][1]);
    if (second > 59)
        return false;
    if (parts[2].length() == 2)
        return true;
    auto second_parts = parts[2].split('.');
    if (second_parts.size() != 2)
        return false;
    if (second_parts[1].length() < 1 || second_parts[1].length() > 3)
        return false;
    for (auto digit : second_parts[1])
        if (!is_ascii_digit(digit))
            return false;

    return true;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#week-number-of-the-last-day
static u32 week_number_of_the_last_day(u64)
{
    // FIXME: sometimes return 53 (!)
    // https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#weeks
    return 52;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-week-string
static bool is_valid_week_string(DeprecatedString const& value)
{
    // A string is a valid week string representing a week-year year and week week if it consists of the following components in the given order:

    // 1. Four or more ASCII digits, representing year, where year > 0
    // 2. A U+002D HYPHEN-MINUS character (-)
    // 3. A U+0057 LATIN CAPITAL LETTER W character (W)
    // 4. Two ASCII digits, representing the week week, in the range 1 ≤ week ≤ maxweek, where maxweek is the week number of the last day of week-year year
    auto parts = value.split('-');
    if (parts.size() != 2)
        return false;
    if (parts[0].length() < 4)
        return false;
    for (auto digit : parts[0])
        if (!is_ascii_digit(digit))
            return false;
    if (parts[1].length() != 3)
        return false;

    if (!parts[1].starts_with('W'))
        return false;
    if (!is_ascii_digit(parts[1][1]))
        return false;
    if (!is_ascii_digit(parts[1][2]))
        return false;

    u64 year = 0;
    for (auto d : parts[0]) {
        year *= 10;
        year += parse_ascii_digit(d);
    }
    auto week = (parse_ascii_digit(parts[1][1]) * 10) + parse_ascii_digit(parts[1][2]);

    return week >= 1 && week <= week_number_of_the_last_day(year);
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-month-string
static bool is_valid_month_string(DeprecatedString const& value)
{
    // A string is a valid month string representing a year year and month month if it consists of the following components in the given order:

    // 1. Four or more ASCII digits, representing year, where year > 0
    // 2. A U+002D HYPHEN-MINUS character (-)
    // 3. Two ASCII digits, representing the month month, in the range 1 ≤ month ≤ 12

    auto parts = value.split('-');
    if (parts.size() != 2)
        return false;

    if (parts[0].length() < 4)
        return false;
    for (auto digit : parts[0])
        if (!is_ascii_digit(digit))
            return false;

    if (parts[1].length() != 2)
        return false;

    if (!is_ascii_digit(parts[1][0]))
        return false;
    if (!is_ascii_digit(parts[1][1]))
        return false;

    auto month = (parse_ascii_digit(parts[1][0]) * 10) + parse_ascii_digit(parts[1][1]);
    return month >= 1 && month <= 12;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-date-string
static bool is_valid_date_string(DeprecatedString const& value)
{
    // A string is a valid date string representing a year year, month month, and day day if it consists of the following components in the given order:

    // 1. A valid month string, representing year and month
    // 2. A U+002D HYPHEN-MINUS character (-)
    // 3. Two ASCII digits, representing day, in the range 1 ≤ day ≤ maxday where maxday is the number of days in the month month and year year
    auto parts = value.split('-');
    if (parts.size() != 3)
        return false;

    if (!is_valid_month_string(DeprecatedString::formatted("{}-{}", parts[0], parts[1])))
        return false;

    if (parts[2].length() != 2)
        return false;

    i64 year = 0;
    for (auto d : parts[0]) {
        year *= 10;
        year += parse_ascii_digit(d);
    }
    auto month = (parse_ascii_digit(parts[1][0]) * 10) + parse_ascii_digit(parts[1][1]);
    i64 day = (parse_ascii_digit(parts[2][0]) * 10) + parse_ascii_digit(parts[2][1]);

    return day >= 1 && day <= AK::days_in_month(year, month);
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-local-date-and-time-string
static bool is_valid_local_date_and_time_string(DeprecatedString const& value)
{
    auto parts_split_by_T = value.split('T');
    if (parts_split_by_T.size() == 2)
        return is_valid_date_string(parts_split_by_T[0]) && is_valid_time_string(parts_split_by_T[1]);
    auto parts_split_by_space = value.split(' ');
    if (parts_split_by_space.size() == 2)
        return is_valid_date_string(parts_split_by_space[0]) && is_valid_time_string(parts_split_by_space[1]);

    return false;
}

// https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#valid-normalised-local-date-and-time-string
static DeprecatedString normalize_local_date_and_time_string(DeprecatedString const& value)
{
    VERIFY(value.count(" "sv) == 1);
    return value.replace(" "sv, "T"sv, ReplaceMode::FirstOnly);
}

// https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
DeprecatedString HTMLInputElement::value_sanitization_algorithm(DeprecatedString value) const
{
    if (type_state() == HTMLInputElement::TypeAttributeState::Text || type_state() == HTMLInputElement::TypeAttributeState::Search || type_state() == HTMLInputElement::TypeAttributeState::Telephone || type_state() == HTMLInputElement::TypeAttributeState::Password) {
        // Strip newlines from the value.
        if (value.contains('\r') || value.contains('\n')) {
            StringBuilder builder;
            for (auto c : value) {
                if (!(c == '\r' || c == '\n'))
                    builder.append(c);
            }
            return builder.to_deprecated_string();
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
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Email) {
        // https://html.spec.whatwg.org/multipage/input.html#email-state-(type=email):value-sanitization-algorithm
        // FIXME: handle the `multiple` attribute
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
        // https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#rules-for-parsing-floating-point-number-values
        // 6. Skip ASCII whitespace within input given position.
        auto maybe_double = value.to_double(TrimWhitespace::Yes);
        if (!maybe_double.has_value() || !isfinite(maybe_double.value()))
            return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Date) {
        // https://html.spec.whatwg.org/multipage/input.html#date-state-(type=date):value-sanitization-algorithm
        if (!is_valid_date_string(value))
            return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Month) {
        // https://html.spec.whatwg.org/multipage/input.html#month-state-(type=month):value-sanitization-algorithm
        if (!is_valid_month_string(value))
            return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Week) {
        // https://html.spec.whatwg.org/multipage/input.html#week-state-(type=week):value-sanitization-algorithm
        if (!is_valid_week_string(value))
            return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Time) {
        // https://html.spec.whatwg.org/multipage/input.html#time-state-(type=time):value-sanitization-algorithm
        if (!is_valid_time_string(value))
            return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::LocalDateAndTime) {
        // https://html.spec.whatwg.org/multipage/input.html#local-date-and-time-state-(type=datetime-local):value-sanitization-algorithm
        if (is_valid_local_date_and_time_string(value))
            return normalize_local_date_and_time_string(value);
        return "";
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Range) {
        // https://html.spec.whatwg.org/multipage/input.html#range-state-(type=range):value-sanitization-algorithm
        auto maybe_double = value.to_double(TrimWhitespace::Yes);
        if (!maybe_double.has_value() || !isfinite(maybe_double.value()))
            return JS::number_to_deprecated_string(maybe_double.value_or(0));
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Color) {
        // https://html.spec.whatwg.org/multipage/input.html#color-state-(type=color):value-sanitization-algorithm
        // If the value of the element is a valid simple color, then set it to the value of the element converted to ASCII lowercase;
        if (is_valid_simple_color(value))
            return value.to_lowercase();
        // otherwise, set it to the string "#000000".
        return "#000000";
    }
    return value;
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:concept-form-reset-control
void HTMLInputElement::reset_algorithm()
{
    // The reset algorithm for input elements is to set the dirty value flag and dirty checkedness flag back to false,
    m_dirty_value = false;
    m_dirty_checkedness = false;

    // set the value of the element to the value of the value content attribute, if there is one, or the empty string otherwise,
    m_value = has_attribute(AttributeNames::value) ? get_attribute(AttributeNames::value) : DeprecatedString::empty();

    // set the checkedness of the element to true if the element has a checked content attribute and false if it does not,
    m_checked = has_attribute(AttributeNames::checked);

    // empty the list of selected files,
    m_selected_files = FileAPI::FileList::create(realm(), {});

    // and then invoke the value sanitization algorithm, if the type attribute's current state defines one.
    m_value = value_sanitization_algorithm(m_value);
    if (m_text_node) {
        m_text_node->set_data(m_value);
        update_placeholder_visibility();
    }
}

void HTMLInputElement::form_associated_element_was_inserted()
{
    create_shadow_tree_if_needed();
}

// https://html.spec.whatwg.org/multipage/input.html#radio-button-group
static bool is_in_same_radio_button_group(HTML::HTMLInputElement const& a, HTML::HTMLInputElement const& b)
{
    auto non_empty_equals = [](auto const& value_a, auto const& value_b) {
        return !value_a.is_empty() && value_a == value_b;
    };
    // The radio button group that contains an input element a also contains all the
    // other input elements b that fulfill all of the following conditions:
    return (
        // - Both a and b are in the same tree.
        // - The input element b's type attribute is in the Radio Button state.
        a.type_state() == b.type_state()
        && b.type_state() == HTMLInputElement::TypeAttributeState::RadioButton
        // - Either a and b have the same form owner, or they both have no form owner.
        && a.form() == b.form()
        // - They both have a name attribute, their name attributes are not empty, and the
        // value of a's name attribute equals the value of b's name attribute.
        && a.has_attribute(HTML::AttributeNames::name)
        && b.has_attribute(HTML::AttributeNames::name)
        && non_empty_equals(a.name(), b.name()));
}

// https://html.spec.whatwg.org/multipage/input.html#radio-button-state-(type=radio)
void HTMLInputElement::set_checked_within_group()
{
    if (checked())
        return;

    set_checked(true, ChangeSource::User);

    // No point iterating the tree if we have an empty name.
    auto name = this->name();
    if (name.is_empty())
        return;

    document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
        if (element.checked() && &element != this && is_in_same_radio_button_group(*this, element))
            element.set_checked(false, ChangeSource::User);
        return IterationDecision::Continue;
    });
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-pre-activation-behavior
void HTMLInputElement::legacy_pre_activation_behavior()
{
    m_before_legacy_pre_activation_behavior_checked = checked();
    m_before_legacy_pre_activation_behavior_indeterminate = indeterminate();

    // 1. If this element's type attribute is in the Checkbox state, then set
    // this element's checkedness to its opposite value (i.e. true if it is
    // false, false if it is true) and set this element's indeterminate IDL
    // attribute to false.
    if (type_state() == TypeAttributeState::Checkbox) {
        set_checked(!checked(), ChangeSource::User);
        set_indeterminate(false);
    }

    // 2. If this element's type attribute is in the Radio Button state, then
    // get a reference to the element in this element's radio button group that
    // has its checkedness set to true, if any, and then set this element's
    // checkedness to true.
    if (type_state() == TypeAttributeState::RadioButton) {
        DeprecatedString name = this->name();

        document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
            if (element.checked() && is_in_same_radio_button_group(*this, element)) {
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
        set_indeterminate(m_before_legacy_pre_activation_behavior_indeterminate);
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
        bool did_reselect_previous_element = false;
        if (m_legacy_pre_activation_behavior_checked_element_in_group) {
            auto& element_in_group = *m_legacy_pre_activation_behavior_checked_element_in_group;
            if (is_in_same_radio_button_group(*this, element_in_group)) {
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

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLInputElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-cva-checkvalidity
WebIDL::ExceptionOr<bool> HTMLInputElement::check_validity()
{
    dbgln("(STUBBED) HTMLInputElement::check_validity(). Called on: {}", debug_description());
    return true;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-cva-reportvalidity
WebIDL::ExceptionOr<bool> HTMLInputElement::report_validity()
{
    dbgln("(STUBBED) HTMLInputElement::report_validity(). Called on: {}", debug_description());
    return true;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-cva-setcustomvalidity
void HTMLInputElement::set_custom_validity(String const& error)
{
    dbgln("(STUBBED) HTMLInputElement::set_custom_validity(error={}). Called on: {}", error, debug_description());
    return;
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-textarea/input-select
WebIDL::ExceptionOr<void> HTMLInputElement::select()
{
    dbgln("(STUBBED) HTMLInputElement::select(). Called on: {}", debug_description());
    return {};
}

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#dom-textarea/input-setselectionrange
WebIDL::ExceptionOr<void> HTMLInputElement::set_selection_range(u32 start, u32 end, Optional<String> const& direction)
{
    dbgln("(STUBBED) HTMLInputElement::set_selection_range(start={}, end={}, direction='{}'). Called on: {}", start, end, direction, debug_description());
    return {};
}

Optional<ARIA::Role> HTMLInputElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-input-button
    if (type_state() == TypeAttributeState::Button)
        return ARIA::Role::button;
    // https://www.w3.org/TR/html-aria/#el-input-checkbox
    if (type_state() == TypeAttributeState::Checkbox)
        return ARIA::Role::checkbox;
    // https://www.w3.org/TR/html-aria/#el-input-email
    if (type_state() == TypeAttributeState::Email && deprecated_attribute("list").is_null())
        return ARIA::Role::textbox;
    // https://www.w3.org/TR/html-aria/#el-input-image
    if (type_state() == TypeAttributeState::ImageButton)
        return ARIA::Role::button;
    // https://www.w3.org/TR/html-aria/#el-input-number
    if (type_state() == TypeAttributeState::Number)
        return ARIA::Role::spinbutton;
    // https://www.w3.org/TR/html-aria/#el-input-radio
    if (type_state() == TypeAttributeState::RadioButton)
        return ARIA::Role::radio;
    // https://www.w3.org/TR/html-aria/#el-input-range
    if (type_state() == TypeAttributeState::Range)
        return ARIA::Role::slider;
    // https://www.w3.org/TR/html-aria/#el-input-reset
    if (type_state() == TypeAttributeState::ResetButton)
        return ARIA::Role::button;
    // https://www.w3.org/TR/html-aria/#el-input-text-list
    if ((type_state() == TypeAttributeState::Text
            || type_state() == TypeAttributeState::Search
            || type_state() == TypeAttributeState::Telephone
            || type_state() == TypeAttributeState::URL
            || type_state() == TypeAttributeState::Email)
        && !deprecated_attribute("list").is_null())
        return ARIA::Role::combobox;
    // https://www.w3.org/TR/html-aria/#el-input-search
    if (type_state() == TypeAttributeState::Search && deprecated_attribute("list").is_null())
        return ARIA::Role::textbox;
    // https://www.w3.org/TR/html-aria/#el-input-submit
    if (type_state() == TypeAttributeState::SubmitButton)
        return ARIA::Role::button;
    // https://www.w3.org/TR/html-aria/#el-input-tel
    if (type_state() == TypeAttributeState::Telephone)
        return ARIA::Role::textbox;
    // https://www.w3.org/TR/html-aria/#el-input-text
    if (type_state() == TypeAttributeState::Text && deprecated_attribute("list").is_null())
        return ARIA::Role::textbox;
    // https://www.w3.org/TR/html-aria/#el-input-url
    if (type_state() == TypeAttributeState::URL && deprecated_attribute("list").is_null())
        return ARIA::Role::textbox;

    // https://www.w3.org/TR/html-aria/#el-input-color
    // https://www.w3.org/TR/html-aria/#el-input-date
    // https://www.w3.org/TR/html-aria/#el-input-datetime-local
    // https://www.w3.org/TR/html-aria/#el-input-file
    // https://www.w3.org/TR/html-aria/#el-input-hidden
    // https://www.w3.org/TR/html-aria/#el-input-month
    // https://www.w3.org/TR/html-aria/#el-input-password
    // https://www.w3.org/TR/html-aria/#el-input-time
    // https://www.w3.org/TR/html-aria/#el-input-week
    return {};
}

bool HTMLInputElement::is_button() const
{
    // https://html.spec.whatwg.org/multipage/input.html#submit-button-state-(type=submit):concept-button
    // https://html.spec.whatwg.org/multipage/input.html#image-button-state-(type=image):concept-button
    // https://html.spec.whatwg.org/multipage/input.html#reset-button-state-(type=reset):concept-button
    // https://html.spec.whatwg.org/multipage/input.html#button-state-(type=button):concept-button
    return type_state() == TypeAttributeState::SubmitButton
        || type_state() == TypeAttributeState::ImageButton
        || type_state() == TypeAttributeState::ResetButton
        || type_state() == TypeAttributeState::Button;
}

bool HTMLInputElement::is_submit_button() const
{
    // https://html.spec.whatwg.org/multipage/input.html#submit-button-state-(type=submit):concept-submit-button
    // https://html.spec.whatwg.org/multipage/input.html#image-button-state-(type=image):concept-submit-button
    return type_state() == TypeAttributeState::SubmitButton
        || type_state() == TypeAttributeState::ImageButton;
}
}
