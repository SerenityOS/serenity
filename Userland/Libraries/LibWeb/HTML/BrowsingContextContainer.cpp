/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

BrowsingContextContainer::BrowsingContextContainer(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

BrowsingContextContainer::~BrowsingContextContainer() = default;

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-nested-browsing-context
void BrowsingContextContainer::create_new_nested_browsing_context()
{
    // 1. Let group be element's node document's browsing context's top-level browsing context's group.
    VERIFY(document().browsing_context());
    auto* group = document().browsing_context()->top_level_browsing_context().group();

    // NOTE: The spec assumes that `group` is non-null here.
    VERIFY(group);
    VERIFY(group->page());

    // 2. Let browsingContext be the result of creating a new browsing context with element's node document, element, and group.
    // 3. Set element's nested browsing context to browsingContext.
    m_nested_browsing_context = BrowsingContext::create_a_new_browsing_context(*group->page(), document(), *this, *group);

    document().browsing_context()->append_child(*m_nested_browsing_context);
    m_nested_browsing_context->set_frame_nesting_levels(document().browsing_context()->frame_nesting_levels());
    m_nested_browsing_context->register_frame_nesting(document().url());

    // 4. If element has a name attribute, then set browsingContext's name to the value of this attribute.
    if (auto name = attribute(HTML::AttributeNames::name); !name.is_empty())
        m_nested_browsing_context->set_name(name);
}

// https://html.spec.whatwg.org/multipage/window-object.html#a-browsing-context-is-discarded
void BrowsingContextContainer::discard_nested_browsing_context()
{
    // 1. Discard all Document objects for all the entries in browsingContext's session history.
    if (m_nested_browsing_context && m_nested_browsing_context->parent())
        m_nested_browsing_context->parent()->remove_child(*m_nested_browsing_context);

    // 2. If browsingContext is a top-level browsing context, then remove browsingContext.
    // NOTE: We skip this here because this is by definition a nested browsing context, not top-level.
}

// https://html.spec.whatwg.org/multipage/browsers.html#concept-bcc-content-document
const DOM::Document* BrowsingContextContainer::content_document() const
{
    // 1. If container's nested browsing context is null, then return null.
    if (m_nested_browsing_context == nullptr)
        return nullptr;

    // 2. Let context be container's nested browsing context.
    auto const& context = *m_nested_browsing_context;

    // 3. Let document be context's active document.
    auto const* document = context.active_document();

    // FIXME: This should not be here, as we're expected to have a document at this point.
    if (!document)
        return nullptr;

    VERIFY(document);
    VERIFY(m_document);

    // 4. If document's origin and container's node document's origin are not same origin-domain, then return null.
    if (!document->origin().is_same_origin_domain(m_document->origin()))
        return nullptr;

    // 5. Return document.
    return document;
}

DOM::Document const* BrowsingContextContainer::content_document_without_origin_check() const
{
    if (!m_nested_browsing_context)
        return nullptr;
    return m_nested_browsing_context->active_document();
}

// https://html.spec.whatwg.org/multipage/embedded-content-other.html#dom-media-getsvgdocument
const DOM::Document* BrowsingContextContainer::get_svg_document() const
{
    // 1. Let document be this element's content document.
    auto const* document = content_document();

    // 2. If document is non-null and was created by the page load processing model for XML files section because the computed type of the resource in the navigate algorithm was image/svg+xml, then return document.
    if (document && document->content_type() == "image/svg+xml"sv)
        return document;
    // 3. Return null.
    return nullptr;
}

HTML::Window* BrowsingContextContainer::content_window() const
{
    // FIXME: This should return the WindowProxy
    auto* document = content_document();
    if (!document)
        return nullptr;
    return const_cast<HTML::Window*>(&document->window());
}

}
