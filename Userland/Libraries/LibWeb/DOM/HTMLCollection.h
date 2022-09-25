/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
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

class HTMLCollection : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(HTMLCollection, Bindings::LegacyPlatformObject);

public:
    static JS::NonnullGCPtr<HTMLCollection> create(ParentNode& root, Function<bool(Element const&)> filter);

    virtual ~HTMLCollection() override;

    size_t length();
    Element* item(size_t index) const;
    Element* named_item(FlyString const& name) const;

    JS::MarkedVector<Element*> collect_matching_elements() const;

    virtual JS::Value item_value(size_t index) const override;
    virtual JS::Value named_item_value(FlyString const& name) const override;
    virtual Vector<String> supported_property_names() const override;
    virtual bool is_supported_property_index(u32) const override;

protected:
    HTMLCollection(ParentNode& root, Function<bool(Element const&)> filter);

    JS::NonnullGCPtr<ParentNode> root() { return *m_root; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<ParentNode> m_root;
    Function<bool(Element const&)> m_filter;
};

}
