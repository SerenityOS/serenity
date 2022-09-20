/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

HashTable<BrowsingContextContainer*>& BrowsingContextContainer::all_instances()
{
    static HashTable<BrowsingContextContainer*> set;
    return set;
}

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

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#matches-about:blank
static bool url_matches_about_blank(AK::URL const& url)
{
    // A URL matches about:blank if its scheme is "about", its path contains a single string "blank", its username and password are the empty string, and its host is null.
    return url.scheme() == "about"sv
        && url.path() == "blank"sv
        && url.username().is_empty()
        && url.password().is_empty()
        && url.host().is_null();
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#shared-attribute-processing-steps-for-iframe-and-frame-elements
void BrowsingContextContainer::shared_attribute_processing_steps_for_iframe_and_frame(bool initial_insertion)
{
    // 1. Let url be the URL record about:blank.
    auto url = AK::URL("about:blank");

    // 2. If element has a src attribute specified, and its value is not the empty string,
    //    then parse the value of that attribute relative to element's node document.
    //    If this is successful, then set url to the resulting URL record.
    auto src_attribute_value = attribute(HTML::AttributeNames::src);
    if (!src_attribute_value.is_null() && !src_attribute_value.is_empty()) {
        auto parsed_src = document().parse_url(src_attribute_value);
        if (parsed_src.is_valid())
            url = parsed_src;
    }

    // 3. If there exists an ancestor browsing context of element's nested browsing context
    //    whose active document's URL, ignoring fragments, is equal to url, then return.
    if (m_nested_browsing_context) {
        for (auto* ancestor = m_nested_browsing_context->parent(); ancestor; ancestor = ancestor->parent()) {
            VERIFY(ancestor->active_document());
            if (ancestor->active_document()->url().equals(url, AK::URL::ExcludeFragment::Yes))
                return;
        }
    }

    // 4. If url matches about:blank and initialInsertion is true, then:
    if (url_matches_about_blank(url) && initial_insertion) {
        // FIXME: 1. Perform the URL and history update steps given element's nested browsing context's active document and url.

        // 2. Run the iframe load event steps given element.
        // FIXME: The spec doesn't check frame vs iframe here. Bug: https://github.com/whatwg/html/issues/8295
        if (is<HTMLIFrameElement>(*this)) {
            run_iframe_load_event_steps(static_cast<HTMLIFrameElement&>(*this));
        }

        // 3. Return.
    }

    // 5. Let resource be a new request whose URL is url and whose referrer policy is the current state of element's referrerpolicy content attribute.
    auto resource = Fetch::Infrastructure::Request();
    resource.set_url(url);
    // FIXME: Set the referrer policy.

    // AD-HOC:
    if (url.protocol() == "file" && document().origin().protocol() != "file") {
        dbgln("iframe failed to load URL: Security violation: {} may not load {}", document().url(), url);
        return;
    }

    // 6. If element is an iframe element, then set element's current navigation was lazy loaded boolean to false.
    if (is<HTMLIFrameElement>(*this)) {
        static_cast<HTMLIFrameElement&>(*this).set_current_navigation_was_lazy_loaded(false);
    }

    // 7. If element is an iframe element, and the will lazy load element steps given element return true, then:
    if (is<HTMLIFrameElement>(*this) && static_cast<HTMLIFrameElement&>(*this).will_lazy_load_element()) {
        // FIXME: 1. Set element's lazy load resumption steps to the rest of this algorithm starting with the step labeled navigate to the resource.
        // FIXME: 2. Set element's current navigation was lazy loaded boolean to true.
        // FIXME: 3. Start intersection-observing a lazy loading element for element.
        // FIXME: 4. Return.
    }

    // 8. Navigate to the resource: navigate an iframe or frame given element and resource.
    navigate_an_iframe_or_frame(move(resource));
}

// https://html.spec.whatwg.org/multipage/iframe-embed-object.html#navigate-an-iframe-or-frame
void BrowsingContextContainer::navigate_an_iframe_or_frame(Fetch::Infrastructure::Request resource)
{
    // 1. Let historyHandling be "default".
    auto history_handling = HistoryHandlingBehavior::Default;

    // 2. If element's nested browsing context's active document is not completely loaded, then set historyHandling to "replace".
    VERIFY(m_nested_browsing_context);
    VERIFY(m_nested_browsing_context->active_document());
    if (!m_nested_browsing_context->active_document()->is_completely_loaded()) {
        history_handling = HistoryHandlingBehavior::Replace;
    }

    // FIXME: 3. Let reportFrameTiming be the following step given response response:
    //           queue an element task on the networking task source
    //           given element's node document's relevant global object
    //           to finalize and report timing given response, element's node document's relevant global object, and element's local name.

    // 4. Navigate element's nested browsing context to resource,
    //    with historyHandling set to historyHandling,
    //    the source browsing context set to element's node document's browsing context,
    //    FIXME: and processResponseEndOfBody set to reportFrameTiming.
    auto* source_browsing_context = document().browsing_context();
    VERIFY(source_browsing_context);
    m_nested_browsing_context->navigate(move(resource), *source_browsing_context, false, history_handling);
}

}
