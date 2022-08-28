/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class BrowsingContextContainer : public HTMLElement {
    WEB_PLATFORM_OBJECT(BrowsingContextContainer, HTMLElement);

public:
    virtual ~BrowsingContextContainer() override;

    BrowsingContext* nested_browsing_context() { return m_nested_browsing_context; }
    BrowsingContext const* nested_browsing_context() const { return m_nested_browsing_context; }

    const DOM::Document* content_document() const;
    DOM::Document const* content_document_without_origin_check() const;

    HTML::Window* content_window() const;

    DOM::Document const* get_svg_document() const;

protected:
    BrowsingContextContainer(DOM::Document&, DOM::QualifiedName);

    void create_new_nested_browsing_context();
    void discard_nested_browsing_context();

    RefPtr<BrowsingContext> m_nested_browsing_context;

private:
    virtual bool is_browsing_context_container() const override { return true; }
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::BrowsingContextContainer>() const { return is_browsing_context_container(); }
}

WRAPPER_HACK(BrowsingContextContainer, Web::HTML)
