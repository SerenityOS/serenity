/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/browsers.html#browsing-context-group-set
static HashTable<BrowsingContextGroup*>& user_agent_browsing_context_group_set()
{
    static HashTable<BrowsingContextGroup*> set;
    return set;
}

BrowsingContextGroup::BrowsingContextGroup(Web::Page& page)
    : m_page(page)
{
    user_agent_browsing_context_group_set().set(this);
}

BrowsingContextGroup::~BrowsingContextGroup()
{
    user_agent_browsing_context_group_set().remove(this);
}

// https://html.spec.whatwg.org/multipage/browsers.html#creating-a-new-browsing-context-group
NonnullRefPtr<BrowsingContextGroup> BrowsingContextGroup::create_a_new_browsing_context_group(Web::Page& page)
{
    // 1. Let group be a new browsing context group.
    // 2. Append group to the user agent's browsing context group set.
    auto group = adopt_ref(*new BrowsingContextGroup(page));

    // 3. Let browsingContext be the result of creating a new browsing context with null, null, and group.
    auto browsing_context = BrowsingContext::create_a_new_browsing_context(page, nullptr, nullptr, group);

    // 4. Append browsingContext to group.
    group->append(move(browsing_context));

    // 5. Return group.
    return group;
}

// https://html.spec.whatwg.org/multipage/browsers.html#bcg-append
void BrowsingContextGroup::append(BrowsingContext& browsing_context)
{
    VERIFY(browsing_context.is_top_level());

    // 1. Append browsingContext to group's browsing context set.
    m_browsing_context_set.set(browsing_context);

    // 2. Set browsingContext's group to group.
    browsing_context.set_group(this);
}

}
