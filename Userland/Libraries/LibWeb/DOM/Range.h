/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/AbstractRange.h>
#include <LibWeb/Selection/Selection.h>

namespace Web::DOM {

enum class RelativeBoundaryPointPosition {
    Equal,
    Before,
    After,
};

// https://dom.spec.whatwg.org/#concept-range-bp-position
RelativeBoundaryPointPosition position_of_boundary_point_relative_to_other_boundary_point(Node const& node_a, u32 offset_a, Node const& node_b, u32 offset_b);

class Range final : public AbstractRange {
    WEB_PLATFORM_OBJECT(Range, AbstractRange);

public:
    [[nodiscard]] static JS::NonnullGCPtr<Range> create(Document&);
    [[nodiscard]] static JS::NonnullGCPtr<Range> create(HTML::Window&);
    [[nodiscard]] static JS::NonnullGCPtr<Range> create(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Range>> construct_impl(JS::Realm&);

    virtual ~Range() override;

    // FIXME: There are a ton of methods missing here.

    WebIDL::ExceptionOr<void> set_start(Node& node, u32 offset);
    WebIDL::ExceptionOr<void> set_end(Node& node, u32 offset);
    WebIDL::ExceptionOr<void> set_start_before(Node& node);
    WebIDL::ExceptionOr<void> set_start_after(Node& node);
    WebIDL::ExceptionOr<void> set_end_before(Node& node);
    WebIDL::ExceptionOr<void> set_end_after(Node& node);
    WebIDL::ExceptionOr<void> select_node(Node& node);
    void collapse(bool to_start);
    WebIDL::ExceptionOr<void> select_node_contents(Node&);

    // https://dom.spec.whatwg.org/#dom-range-start_to_start
    enum HowToCompareBoundaryPoints : u16 {
        START_TO_START = 0,
        START_TO_END = 1,
        END_TO_END = 2,
        END_TO_START = 3,
    };

    WebIDL::ExceptionOr<i16> compare_boundary_points(u16 how, Range const& source_range) const;

    JS::NonnullGCPtr<Range> inverted() const;
    JS::NonnullGCPtr<Range> normalized() const;
    JS::NonnullGCPtr<Range> clone_range() const;

    JS::NonnullGCPtr<Node> common_ancestor_container() const;

    // https://dom.spec.whatwg.org/#dom-range-detach
    void detach() const
    {
        // The detach() method steps are to do nothing.
        // Note: Its functionality (disabling a Range object) was removed, but the method itself is preserved for compatibility.
    }

    bool intersects_node(Node const&) const;
    WebIDL::ExceptionOr<bool> is_point_in_range(Node const&, u32 offset) const;
    WebIDL::ExceptionOr<i16> compare_point(Node const&, u32 offset) const;

    WebIDL::ExceptionOr<void> delete_contents();
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> extract_contents();
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> clone_contents();

    WebIDL::ExceptionOr<void> insert_node(JS::NonnullGCPtr<Node>);
    WebIDL::ExceptionOr<void> surround_contents(JS::NonnullGCPtr<Node> new_parent);

    String to_string() const;

    static HashTable<Range*>& live_ranges();

    JS::NonnullGCPtr<Geometry::DOMRect> get_bounding_client_rect() const;

    bool contains_node(Node const&) const;

    void set_associated_selection(Badge<Selection::Selection>, JS::GCPtr<Selection::Selection>);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> create_contextual_fragment(String const& fragment);

private:
    explicit Range(Document&);
    Range(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    Node& root();
    Node const& root() const;

    void update_associated_selection();

    enum class StartOrEnd {
        Start,
        End,
    };

    WebIDL::ExceptionOr<void> set_start_or_end(Node& node, u32 offset, StartOrEnd start_or_end);
    WebIDL::ExceptionOr<void> select(Node& node);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> extract();
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentFragment>> clone_the_contents();
    WebIDL::ExceptionOr<void> insert(JS::NonnullGCPtr<Node>);

    bool partially_contains_node(Node const&) const;

    JS::GCPtr<Selection::Selection> m_associated_selection;
};

}
