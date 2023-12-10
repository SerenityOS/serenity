/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLProgressElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLProgressElement, HTMLElement);
    JS_DECLARE_ALLOCATOR(HTMLProgressElement);

public:
    virtual ~HTMLProgressElement() override;

    double value() const;
    WebIDL::ExceptionOr<void> set_value(double);

    double max() const;
    WebIDL::ExceptionOr<void> set_max(double value);

    double position() const;

    // ^HTMLElement
    virtual void inserted() override;
    virtual void removed_from(DOM::Node*) override;

    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    // https://www.w3.org/TR/html-aria/#el-progress
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::progressbar; }

private:
    HTMLProgressElement(DOM::Document&, DOM::QualifiedName);

    // ^DOM::Node
    virtual bool is_html_progress_element() const final { return true; }

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    void create_shadow_tree_if_needed();

    void update_progress_value_element();

    bool is_determinate() const { return has_attribute(HTML::AttributeNames::value); }

    JS::GCPtr<DOM::Element> m_progress_value_element;
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLProgressElement>() const { return is_html_progress_element(); }
}
