/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTemplateElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTemplateElementWrapper;

    HTMLTemplateElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTemplateElement() override;

    NonnullRefPtr<DOM::DocumentFragment> content() { return *m_content; }
    const NonnullRefPtr<DOM::DocumentFragment> content() const { return *m_content; }

    virtual void adopted_from(DOM::Document&) override;
    virtual void cloned(Node& copy, bool clone_children) override;

private:
    DOM::Document& appropriate_template_contents_owner_document(DOM::Document&);

    RefPtr<DOM::DocumentFragment> m_content;
};

}
