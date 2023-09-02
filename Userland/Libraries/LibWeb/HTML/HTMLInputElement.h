/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/FileAPI/FileList.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/input.html#attr-input-type
#define ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES                                  \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(hidden, Hidden)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(text, Text)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(search, Search)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(tel, Telephone)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(url, URL)                           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(email, Email)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(password, Password)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(date, Date)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(month, Month)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(week, Week)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(time, Time)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE("datetime-local", LocalDateAndTime) \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(number, Number)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(range, Range)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(color, Color)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(checkbox, Checkbox)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(radio, RadioButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(file, FileUpload)                   \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(submit, SubmitButton)               \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(image, ImageButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(reset, ResetButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(button, Button)

class HTMLInputElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLInputElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLInputElement)

public:
    virtual ~HTMLInputElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    enum class TypeAttributeState {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(_, state) state,
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    };

    DeprecatedString type() const;
    TypeAttributeState type_state() const { return m_type; }
    WebIDL::ExceptionOr<void> set_type(DeprecatedString const&);

    DeprecatedString default_value() const { return attribute(HTML::AttributeNames::value); }
    DeprecatedString name() const { return attribute(HTML::AttributeNames::name); }

    virtual DeprecatedString value() const override;
    WebIDL::ExceptionOr<void> set_value(DeprecatedString);

    Optional<DeprecatedString> placeholder_value() const;

    bool checked() const { return m_checked; }
    enum class ChangeSource {
        Programmatic,
        User,
    };
    void set_checked(bool, ChangeSource = ChangeSource::Programmatic);

    bool checked_binding() const { return checked(); }
    void set_checked_binding(bool);

    bool indeterminate() const { return m_indeterminate; }
    void set_indeterminate(bool);

    void did_edit_text_node(Badge<BrowsingContext>);

    JS::GCPtr<FileAPI::FileList> files();
    void set_files(JS::GCPtr<FileAPI::FileList>);

    // NOTE: User interaction
    // https://html.spec.whatwg.org/multipage/input.html#update-the-file-selection
    void update_the_file_selection(JS::NonnullGCPtr<FileAPI::FileList>);

    WebIDL::ExceptionOr<bool> check_validity();
    WebIDL::ExceptionOr<bool> report_validity();
    void set_custom_validity(DeprecatedString const&);

    WebIDL::ExceptionOr<void> select();
    WebIDL::ExceptionOr<void> set_selection_range(u32 start, u32 end, DeprecatedString const& direction);

    WebIDL::ExceptionOr<void> show_picker();

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-input-element
    virtual bool is_focusable() const override { return m_type != TypeAttributeState::Hidden; }

    // ^HTMLElement
    virtual void attribute_changed(DeprecatedFlyString const&, DeprecatedString const&) override;

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-submit
    virtual bool is_submittable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-reset
    virtual bool is_resettable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#concept-button
    virtual bool is_button() const override;

    // https://html.spec.whatwg.org/multipage/forms.html#concept-submit-button
    virtual bool is_submit_button() const override;

    virtual void reset_algorithm() override;

    virtual void form_associated_element_was_inserted() override;

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return type_state() != TypeAttributeState::Hidden; }

    virtual Optional<ARIA::Role> default_role() const override;

    JS::GCPtr<Element> placeholder_element() { return m_placeholder_element; }
    JS::GCPtr<Element const> placeholder_element() const { return m_placeholder_element; }

private:
    HTMLInputElement(DOM::Document&, DOM::QualifiedName);

    // ^DOM::Node
    virtual bool is_html_input_element() const final { return true; }

    // ^DOM::EventTarget
    virtual void did_lose_focus() override;
    virtual void did_receive_focus() override;
    virtual void legacy_pre_activation_behavior() override;
    virtual void legacy_cancelled_activation_behavior() override;
    virtual void legacy_cancelled_activation_behavior_was_not_called() override;

    // ^DOM::Element
    virtual i32 default_tab_index_value() const override;

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    static TypeAttributeState parse_type_attribute(StringView);
    void create_shadow_tree_if_needed();
    WebIDL::ExceptionOr<void> run_input_activation_behavior();
    void set_checked_within_group();

    void handle_readonly_attribute(DeprecatedFlyString const& value);

    // https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
    DeprecatedString value_sanitization_algorithm(DeprecatedString) const;

    void update_placeholder_visibility();
    JS::GCPtr<DOM::Element> m_placeholder_element;
    JS::GCPtr<DOM::Text> m_placeholder_text_node;

    JS::GCPtr<DOM::Element> m_inner_text_element;
    JS::GCPtr<DOM::Text> m_text_node;
    bool m_checked { false };

    // https://html.spec.whatwg.org/multipage/input.html#dom-input-indeterminate
    bool m_indeterminate { false };

    // https://html.spec.whatwg.org/multipage/input.html#concept-input-checked-dirty-flag
    bool m_dirty_checkedness { false };

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-dirty
    bool m_dirty_value { false };

    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:concept-fe-mutable
    bool m_is_mutable { true };

    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-pre-activation-behavior
    bool m_before_legacy_pre_activation_behavior_checked { false };
    bool m_before_legacy_pre_activation_behavior_indeterminate { false };
    JS::GCPtr<HTMLInputElement> m_legacy_pre_activation_behavior_checked_element_in_group;

    // https://html.spec.whatwg.org/multipage/input.html#concept-input-type-file-selected
    JS::GCPtr<FileAPI::FileList> m_selected_files;

    TypeAttributeState m_type { TypeAttributeState::Text };
    DeprecatedString m_value;
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLInputElement>() const { return is_html_input_element(); }
}
