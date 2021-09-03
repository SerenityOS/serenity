/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/FlyString.h>
#include <YAK/Function.h>
#include <YAK/Noncopyable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// NOTE: HTMLCollection is in the DOM namespace because it's part of the DOM specification.

// This class implements a live, filtered view of a DOM subtree.
// When constructing an HTMLCollection, you provide a root node + a filter.
// The filter is a simple Function object that answers the question
// "is this Element part of the collection?"

// FIXME: HTMLCollection currently does no caching. It will re-filter on every access!
//        We should teach it how to cache results. The main challenge is invalidating
//        these caches, since this needs to happen on various kinds of DOM mutation.

class HTMLCollection
    : public RefCounted<HTMLCollection>
    , public Bindings::Wrappable {
    YAK_MAKE_NONCOPYABLE(HTMLCollection);
    YAK_MAKE_NONMOVABLE(HTMLCollection);

public:
    using WrapperType = Bindings::HTMLCollectionWrapper;

    static NonnullRefPtr<HTMLCollection> create(ParentNode& root, Function<bool(Element const&)> filter)
    {
        return adopt_ref(*new HTMLCollection(root, move(filter)));
    }

    ~HTMLCollection();

    size_t length();
    Element* item(size_t index);
    Element* named_item(FlyString const& name);

    Vector<NonnullRefPtr<Element>> collect_matching_elements();

protected:
    HTMLCollection(ParentNode& root, Function<bool(Element const&)> filter);

private:
    NonnullRefPtr<ParentNode> m_root;
    Function<bool(Element const&)> m_filter;
};

}

namespace Web::Bindings {

HTMLCollectionWrapper* wrap(JS::GlobalObject&, DOM::HTMLCollection&);

}
