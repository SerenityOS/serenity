/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class BrowsingContextGroup : public RefCounted<BrowsingContextGroup> {
public:
    static NonnullRefPtr<BrowsingContextGroup> create_a_new_browsing_context_group(Page&);
    ~BrowsingContextGroup();

    Page* page() { return m_page; }
    Page const* page() const { return m_page; }

    auto& browsing_context_set() { return m_browsing_context_set; }
    auto const& browsing_context_set() const { return m_browsing_context_set; }

    void append(BrowsingContext&);
    void remove(BrowsingContext&);

private:
    explicit BrowsingContextGroup(Web::Page&);

    // https://html.spec.whatwg.org/multipage/browsers.html#browsing-context-group-set
    OrderedHashTable<NonnullRefPtr<BrowsingContext>> m_browsing_context_set;

    WeakPtr<Page> m_page;
};

}
