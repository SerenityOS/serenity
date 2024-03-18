/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibURL/Forward.h>

namespace Manual {

class PageNode;

class Node : public RefCounted<Node> {
public:
    virtual ~Node() = default;

    virtual ErrorOr<Span<NonnullRefPtr<Node const>>> children() const = 0;
    virtual Node const* parent() const = 0;
    virtual ErrorOr<String> name() const = 0;
    virtual bool is_page() const { return false; }
    virtual bool is_open() const { return false; }
    virtual ErrorOr<String> path() const = 0;
    virtual PageNode const* document() const = 0;
    virtual unsigned section_number() const = 0;

    // Backend for the command-line argument format that Help and man accept. Handles:
    // [/path/to/documentation.md] (no second argument)
    // [page] (no second argument) - will find first section with that page
    // [section] [page]
    // Help can also (externally) handle search queries, which is not possible (yet) in man.
    static ErrorOr<NonnullRefPtr<PageNode const>> try_create_from_query(Vector<StringView, 2> const& query_parameters);

    // Finds a page via the help://man/<number>/<subsections...>/page URLs.
    // This will automatically start discovering pages by inspecting the filesystem.
    static ErrorOr<NonnullRefPtr<Node const>> try_find_from_help_url(URL::URL const&);

    bool operator==(Node const& other) const
    {
        if (auto this_path = this->path(), other_path = other.path();
            !this_path.is_error() && !other_path.is_error()) {
            return this_path.release_value() == other_path.release_value();
        }
        return false;
    }
};

}

namespace AK {

template<typename T>
requires(IsBaseOf<Manual::Node, T>) struct Traits<T> : public DefaultTraits<T> {
    static unsigned hash(T p) { return Traits::hash(p.path()); }
};

}
