/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/AbstractRange.h>

namespace Web::DOM {

class Range final : public AbstractRange {
    JS_OBJECT(Range, AbstractRange);

public:
    Range& impl() { return *this; }

    static JS::NonnullGCPtr<Range> create(Document&);
    static JS::NonnullGCPtr<Range> create(HTML::Window&);
    static JS::NonnullGCPtr<Range> create(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
    static JS::NonnullGCPtr<Range> create_with_global_object(Bindings::WindowObject&);

    explicit Range(Document&);
    Range(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
    virtual ~Range() override;

    // FIXME: There are a ton of methods missing here.

    ExceptionOr<void> set_start(Node& node, u32 offset);
    ExceptionOr<void> set_end(Node& node, u32 offset);
    ExceptionOr<void> set_start_before(Node& node);
    ExceptionOr<void> set_start_after(Node& node);
    ExceptionOr<void> set_end_before(Node& node);
    ExceptionOr<void> set_end_after(Node& node);
    ExceptionOr<void> select_node(Node& node);
    void collapse(bool to_start);
    ExceptionOr<void> select_node_contents(Node const&);

    // https://dom.spec.whatwg.org/#dom-range-start_to_start
    enum HowToCompareBoundaryPoints : u16 {
        START_TO_START = 0,
        START_TO_END = 1,
        END_TO_END = 2,
        END_TO_START = 3,
    };

    ExceptionOr<i16> compare_boundary_points(u16 how, Range const& source_range) const;

    JS::NonnullGCPtr<Range> inverted() const;
    JS::NonnullGCPtr<Range> normalized() const;
    JS::NonnullGCPtr<Range> clone_range() const;

    NonnullRefPtr<Node> common_ancestor_container() const;

    // https://dom.spec.whatwg.org/#dom-range-detach
    void detach() const
    {
        // The detach() method steps are to do nothing.
        // Note: Its functionality (disabling a Range object) was removed, but the method itself is preserved for compatibility.
    }

    bool intersects_node(Node const&) const;
    ExceptionOr<bool> is_point_in_range(Node const&, u32 offset) const;
    ExceptionOr<i16> compare_point(Node const&, u32 offset) const;

    ExceptionOr<void> delete_contents();
    ExceptionOr<NonnullRefPtr<DocumentFragment>> extract_contents();
    ExceptionOr<NonnullRefPtr<DocumentFragment>> clone_contents();

    ExceptionOr<void> insert_node(NonnullRefPtr<Node>);
    ExceptionOr<void> surround_contents(NonnullRefPtr<Node> new_parent);

    String to_string() const;

    static HashTable<Range*>& live_ranges();

private:
    Node& root();
    Node const& root() const;

    enum class StartOrEnd {
        Start,
        End,
    };

    ExceptionOr<void> set_start_or_end(Node& node, u32 offset, StartOrEnd start_or_end);
    ExceptionOr<void> select(Node& node);

    ExceptionOr<NonnullRefPtr<DocumentFragment>> extract();
    ExceptionOr<NonnullRefPtr<DocumentFragment>> clone_the_contents();
    ExceptionOr<void> insert(NonnullRefPtr<Node>);

    bool contains_node(Node const&) const;
    bool partially_contains_node(Node const&) const;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::Range& object) { return &object; }
using RangeWrapper = Web::DOM::Range;
}
