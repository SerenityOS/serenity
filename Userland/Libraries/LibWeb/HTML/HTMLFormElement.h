/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/AbstractBrowsingContext.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#attr-fs-method
#define ENUMERATE_FORM_METHOD_ATTRIBUTES          \
    __ENUMERATE_FORM_METHOD_ATTRIBUTE(get, GET)   \
    __ENUMERATE_FORM_METHOD_ATTRIBUTE(post, POST) \
    __ENUMERATE_FORM_METHOD_ATTRIBUTE(dialog, Dialog)

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#attr-fs-enctype
#define ENUMERATE_FORM_METHOD_ENCODING_TYPES                                                   \
    __ENUMERATE_FORM_METHOD_ENCODING_TYPE("application/x-www-form-urlencoded", FormUrlEncoded) \
    __ENUMERATE_FORM_METHOD_ENCODING_TYPE("multipart/form-data", FormData)                     \
    __ENUMERATE_FORM_METHOD_ENCODING_TYPE("text/plain", PlainText)

class HTMLFormElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLFormElement, HTMLElement);

public:
    virtual ~HTMLFormElement() override;

    DeprecatedString action_from_form_element(JS::NonnullGCPtr<HTMLElement> element) const;

    enum class MethodAttributeState {
#define __ENUMERATE_FORM_METHOD_ATTRIBUTE(_, state) state,
        ENUMERATE_FORM_METHOD_ATTRIBUTES
#undef __ENUMERATE_FORM_METHOD_ATTRIBUTE
    };

    MethodAttributeState method_state_from_form_element(JS::NonnullGCPtr<HTMLElement const> element) const;

    enum class EncodingTypeAttributeState {
#define __ENUMERATE_FORM_METHOD_ENCODING_TYPE(_, state) state,
        ENUMERATE_FORM_METHOD_ENCODING_TYPES
#undef __ENUMERATE_FORM_METHOD_ENCODING_TYPE
    };

    EncodingTypeAttributeState encoding_type_state_from_form_element(JS::NonnullGCPtr<HTMLElement> element) const;

    WebIDL::ExceptionOr<void> submit_form(JS::NonnullGCPtr<HTMLElement> submitter, bool from_submit_binding = false);

    void reset_form();

    // NOTE: This is for the JS bindings. Use submit_form instead.
    WebIDL::ExceptionOr<void> submit();

    // NOTE: This is for the JS bindings. Use submit_form instead.
    void reset();

    void add_associated_element(Badge<FormAssociatedElement>, HTMLElement&);
    void remove_associated_element(Badge<FormAssociatedElement>, HTMLElement&);

    ErrorOr<Vector<JS::NonnullGCPtr<DOM::Element>>> get_submittable_elements();

    JS::NonnullGCPtr<DOM::HTMLFormControlsCollection> elements() const;
    unsigned length() const;

    WebIDL::ExceptionOr<bool> check_validity();
    WebIDL::ExceptionOr<bool> report_validity();

    // https://www.w3.org/TR/html-aria/#el-form
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::form; }

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#constructing-entry-list
    bool constructing_entry_list() const { return m_constructing_entry_list; }
    void set_constructing_entry_list(bool value) { m_constructing_entry_list = value; }

    StringView method() const;
    WebIDL::ExceptionOr<void> set_method(String const&);

    String action() const;
    WebIDL::ExceptionOr<void> set_action(String const&);

private:
    HTMLFormElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    ErrorOr<void> populate_vector_with_submittable_elements_in_tree_order(JS::NonnullGCPtr<DOM::Element> element, Vector<JS::NonnullGCPtr<DOM::Element>>& elements);

    ErrorOr<String> pick_an_encoding() const;

    ErrorOr<void> mutate_action_url(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);
    ErrorOr<void> submit_as_entity_body(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, EncodingTypeAttributeState encoding_type, String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);
    void get_action_url(AK::URL parsed_action, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);
    ErrorOr<void> mail_with_headers(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);
    ErrorOr<void> mail_as_body(AK::URL parsed_action, Vector<XHR::FormDataEntry> entry_list, EncodingTypeAttributeState encoding_type, String encoding, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);
    void plan_to_navigate_to(AK::URL url, Variant<Empty, String, POSTResource> post_resource, JS::NonnullGCPtr<Navigable> target_navigable, HistoryHandlingBehavior history_handling);

    bool m_firing_submission_events { false };

    // https://html.spec.whatwg.org/multipage/forms.html#locked-for-reset
    bool m_locked_for_reset { false };

    Vector<JS::GCPtr<HTMLElement>> m_associated_elements;

    JS::GCPtr<DOM::HTMLFormControlsCollection> mutable m_elements;

    bool m_constructing_entry_list { false };

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#planned-navigation
    // Each form element has a planned navigation, which is either null or a task; when the form is first created,
    // its planned navigation must be set to null.
    Task const* m_planned_navigation { nullptr };
};

}
