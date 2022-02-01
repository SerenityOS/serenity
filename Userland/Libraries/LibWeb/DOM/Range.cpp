/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

NonnullRefPtr<Range> Range::create(Window& window)
{
    return Range::create(window.associated_document());
}

NonnullRefPtr<Range> Range::create(Document& document)
{
    return adopt_ref(*new Range(document));
}

NonnullRefPtr<Range> Range::create(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
{
    return adopt_ref(*new Range(start_container, start_offset, end_container, end_offset));
}

NonnullRefPtr<Range> Range::create_with_global_object(Bindings::WindowObject& window)
{
    return Range::create(window.impl());
}

Range::Range(Document& document)
    : Range(document, 0, document, 0)
{
}

Range::Range(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
    : AbstractRange(start_container, start_offset, end_container, end_offset)
{
}

Range::~Range()
{
}

// https://dom.spec.whatwg.org/#concept-range-root
Node& Range::root()
{
    // The root of a live range is the root of its start node.
    return m_start_container->root();
}

Node const& Range::root() const
{
    return m_start_container->root();
}

enum class RelativeBoundaryPointPosition {
    Equal,
    Before,
    After,
};

// https://dom.spec.whatwg.org/#concept-range-bp-position
static RelativeBoundaryPointPosition position_of_boundary_point_relative_to_other_boundary_point(Node const& node_a, u32 offset_a, Node const& node_b, u32 offset_b)
{
    // 1. Assert: nodeA and nodeB have the same root.
    VERIFY(&node_a.root() == &node_b.root());

    // 2. If nodeA is nodeB, then return equal if offsetA is offsetB, before if offsetA is less than offsetB, and after if offsetA is greater than offsetB.
    if (&node_a == &node_b) {
        if (offset_a == offset_b)
            return RelativeBoundaryPointPosition::Equal;

        if (offset_a < offset_b)
            return RelativeBoundaryPointPosition::Before;

        return RelativeBoundaryPointPosition::After;
    }

    // 3. If nodeA is following nodeB, then if the position of (nodeB, offsetB) relative to (nodeA, offsetA) is before, return after, and if it is after, return before.
    if (node_a.is_following(node_b)) {
        auto relative_position = position_of_boundary_point_relative_to_other_boundary_point(node_b, offset_b, node_a, offset_a);

        if (relative_position == RelativeBoundaryPointPosition::Before)
            return RelativeBoundaryPointPosition::After;

        if (relative_position == RelativeBoundaryPointPosition::After)
            return RelativeBoundaryPointPosition::Before;
    }

    // 4. If nodeA is an ancestor of nodeB:
    if (node_a.is_ancestor_of(node_b)) {
        // 1. Let child be nodeB.
        NonnullRefPtr<Node> child = node_b;

        // 2. While child is not a child of nodeA, set child to its parent.
        while (!node_a.is_parent_of(child)) {
            auto* parent = child->parent();
            VERIFY(parent);
            child = *parent;
        }

        // 3. If child’s index is less than offsetA, then return after.
        if (child->index() < offset_a)
            return RelativeBoundaryPointPosition::After;
    }

    // 5. Return before.
    return RelativeBoundaryPointPosition::Before;
}

ExceptionOr<void> Range::set_start_or_end(Node& node, u32 offset, StartOrEnd start_or_end)
{
    // To set the start or end of a range to a boundary point (node, offset), run these steps:

    // 1. If node is a doctype, then throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(node))
        return InvalidNodeTypeError::create("Node cannot be a DocumentType.");

    // 2. If offset is greater than node’s length, then throw an "IndexSizeError" DOMException.
    if (offset > node.length())
        return IndexSizeError::create(String::formatted("Node does not contain a child at offset {}", offset));

    // 3. Let bp be the boundary point (node, offset).

    if (start_or_end == StartOrEnd::Start) {
        // -> If these steps were invoked as "set the start"

        // 1. If range’s root is not equal to node’s root, or if bp is after the range’s end, set range’s end to bp.
        if (&root() != &node.root() || position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_end_container, m_end_offset) == RelativeBoundaryPointPosition::After) {
            m_end_container = node;
            m_end_offset = offset;
        }

        // 2. Set range’s start to bp.
        m_start_container = node;
        m_start_offset = offset;
    } else {
        // -> If these steps were invoked as "set the end"
        VERIFY(start_or_end == StartOrEnd::End);

        // 1. If range’s root is not equal to node’s root, or if bp is before the range’s start, set range’s start to bp.
        if (&root() != &node.root() || position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_start_container, m_start_offset) == RelativeBoundaryPointPosition::Before) {
            m_start_container = node;
            m_start_offset = offset;
        }

        // 2. Set range’s end to bp.
        m_end_container = node;
        m_end_offset = offset;
    }

    return {};
}

// https://dom.spec.whatwg.org/#concept-range-bp-set
ExceptionOr<void> Range::set_start(Node& node, u32 offset)
{
    // The setStart(node, offset) method steps are to set the start of this to boundary point (node, offset).
    return set_start_or_end(node, offset, StartOrEnd::Start);
}

ExceptionOr<void> Range::set_end(Node& node, u32 offset)
{
    // The setEnd(node, offset) method steps are to set the end of this to boundary point (node, offset).
    return set_start_or_end(node, offset, StartOrEnd::End);
}

// https://dom.spec.whatwg.org/#dom-range-setstartbefore
ExceptionOr<void> Range::set_start_before(Node& node)
{
    // 1. Let parent be node’s parent.
    auto* parent = node.parent();

    // 2. If parent is null, then throw an "InvalidNodeTypeError" DOMException.
    if (!parent)
        return InvalidNodeTypeError::create("Given node has no parent.");

    // 3. Set the start of this to boundary point (parent, node’s index).
    return set_start_or_end(*parent, node.index(), StartOrEnd::Start);
}

// https://dom.spec.whatwg.org/#dom-range-setstartafter
ExceptionOr<void> Range::set_start_after(Node& node)
{
    // 1. Let parent be node’s parent.
    auto* parent = node.parent();

    // 2. If parent is null, then throw an "InvalidNodeTypeError" DOMException.
    if (!parent)
        return InvalidNodeTypeError::create("Given node has no parent.");

    // 3. Set the start of this to boundary point (parent, node’s index plus 1).
    return set_start_or_end(*parent, node.index() + 1, StartOrEnd::Start);
}

// https://dom.spec.whatwg.org/#dom-range-setendbefore
ExceptionOr<void> Range::set_end_before(Node& node)
{
    // 1. Let parent be node’s parent.
    auto* parent = node.parent();

    // 2. If parent is null, then throw an "InvalidNodeTypeError" DOMException.
    if (!parent)
        return InvalidNodeTypeError::create("Given node has no parent.");

    // 3. Set the end of this to boundary point (parent, node’s index).
    return set_start_or_end(*parent, node.index(), StartOrEnd::End);
}

// https://dom.spec.whatwg.org/#dom-range-setendafter
ExceptionOr<void> Range::set_end_after(Node& node)
{
    // 1. Let parent be node’s parent.
    auto* parent = node.parent();

    // 2. If parent is null, then throw an "InvalidNodeTypeError" DOMException.
    if (!parent)
        return InvalidNodeTypeError::create("Given node has no parent.");

    // 3. Set the end of this to boundary point (parent, node’s index plus 1).
    return set_start_or_end(*parent, node.index() + 1, StartOrEnd::End);
}

// https://dom.spec.whatwg.org/#dom-range-compareboundarypoints
ExceptionOr<i16> Range::compare_boundary_points(u16 how, Range const& source_range) const
{
    // 1. If how is not one of
    //      - START_TO_START,
    //      - START_TO_END,
    //      - END_TO_END, and
    //      - END_TO_START,
    //    then throw a "NotSupportedError" DOMException.
    if (how != HowToCompareBoundaryPoints::START_TO_START && how != HowToCompareBoundaryPoints::START_TO_END && how != HowToCompareBoundaryPoints::END_TO_END && how != HowToCompareBoundaryPoints::END_TO_START)
        return NotSupportedError::create(String::formatted("Expected 'how' to be one of START_TO_START (0), START_TO_END (1), END_TO_END (2) or END_TO_START (3), got {}", how));

    // 2. If this’s root is not the same as sourceRange’s root, then throw a "WrongDocumentError" DOMException.
    if (&root() != &source_range.root())
        return WrongDocumentError::create("This range is not in the same tree as the source range.");

    RefPtr<Node> this_point_node;
    u32 this_point_offset = 0;

    RefPtr<Node> other_point_node;
    u32 other_point_offset = 0;

    // 3. If how is:
    switch (how) {
    case HowToCompareBoundaryPoints::START_TO_START:
        // -> START_TO_START:
        //    Let this point be this’s start. Let other point be sourceRange’s start.
        this_point_node = m_start_container;
        this_point_offset = m_start_offset;

        other_point_node = source_range.m_start_container;
        other_point_offset = source_range.m_start_offset;
        break;
    case HowToCompareBoundaryPoints::START_TO_END:
        // -> START_TO_END:
        //    Let this point be this’s end. Let other point be sourceRange’s start.
        this_point_node = m_end_container;
        this_point_offset = m_end_offset;

        other_point_node = source_range.m_start_container;
        other_point_offset = source_range.m_start_offset;
        break;
    case HowToCompareBoundaryPoints::END_TO_END:
        // -> END_TO_END:
        //    Let this point be this’s end. Let other point be sourceRange’s end.
        this_point_node = m_end_container;
        this_point_offset = m_end_offset;

        other_point_node = source_range.m_end_container;
        other_point_offset = source_range.m_end_offset;
        break;
    case HowToCompareBoundaryPoints::END_TO_START:
        // -> END_TO_START:
        //    Let this point be this’s start. Let other point be sourceRange’s end.
        this_point_node = m_start_container;
        this_point_offset = m_start_offset;

        other_point_node = source_range.m_end_container;
        other_point_offset = source_range.m_end_offset;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    VERIFY(this_point_node);
    VERIFY(other_point_node);

    // 4. If the position of this point relative to other point is
    auto relative_position = position_of_boundary_point_relative_to_other_boundary_point(*this_point_node, this_point_offset, *other_point_node, other_point_offset);
    switch (relative_position) {
    case RelativeBoundaryPointPosition::Before:
        // -> before
        //    Return −1.
        return -1;
    case RelativeBoundaryPointPosition::Equal:
        // -> equal
        //    Return 0.
        return 0;
    case RelativeBoundaryPointPosition::After:
        // -> after
        //    Return 1.
        return 1;
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://dom.spec.whatwg.org/#concept-range-select
ExceptionOr<void> Range::select(Node& node)
{
    // 1. Let parent be node’s parent.
    auto* parent = node.parent();

    // 2. If parent is null, then throw an "InvalidNodeTypeError" DOMException.
    if (!parent)
        return InvalidNodeTypeError::create("Given node has no parent.");

    // 3. Let index be node’s index.
    auto index = node.index();

    // 4. Set range’s start to boundary point (parent, index).
    m_start_container = *parent;
    m_start_offset = index;

    // 5. Set range’s end to boundary point (parent, index plus 1).
    m_end_container = *parent;
    m_end_offset = index + 1;

    return {};
}

// https://dom.spec.whatwg.org/#dom-range-selectnode
ExceptionOr<void> Range::select_node(Node& node)
{
    // The selectNode(node) method steps are to select node within this.
    return select(node);
}

// https://dom.spec.whatwg.org/#dom-range-collapse
void Range::collapse(bool to_start)
{
    // The collapse(toStart) method steps are to, if toStart is true, set end to start; otherwise set start to end.
    if (to_start) {
        m_end_container = m_start_container;
        m_end_offset = m_start_offset;
        return;
    }

    m_start_container = m_end_container;
    m_start_offset = m_end_offset;
}

// https://dom.spec.whatwg.org/#dom-range-selectnodecontents
ExceptionOr<void> Range::select_node_contents(Node const& node)
{
    // 1. If node is a doctype, throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(node))
        return InvalidNodeTypeError::create("Node cannot be a DocumentType.");

    // 2. Let length be the length of node.
    auto length = node.length();

    // 3. Set start to the boundary point (node, 0).
    m_start_container = node;
    m_start_offset = 0;

    // 4. Set end to the boundary point (node, length).
    m_end_container = node;
    m_end_offset = length;

    return {};
}

NonnullRefPtr<Range> Range::clone_range() const
{
    return adopt_ref(*new Range(const_cast<Node&>(*m_start_container), m_start_offset, const_cast<Node&>(*m_end_container), m_end_offset));
}

NonnullRefPtr<Range> Range::inverted() const
{
    return adopt_ref(*new Range(const_cast<Node&>(*m_end_container), m_end_offset, const_cast<Node&>(*m_start_container), m_start_offset));
}

NonnullRefPtr<Range> Range::normalized() const
{
    if (m_start_container.ptr() == m_end_container.ptr()) {
        if (m_start_offset <= m_end_offset)
            return clone_range();

        return inverted();
    }

    if (m_start_container->is_before(m_end_container))
        return clone_range();

    return inverted();
}

// https://dom.spec.whatwg.org/#dom-range-commonancestorcontainer
NonnullRefPtr<Node> Range::common_ancestor_container() const
{
    // 1. Let container be start node.
    auto container = m_start_container;

    // 2. While container is not an inclusive ancestor of end node, let container be container’s parent.
    while (!container->is_inclusive_ancestor_of(m_end_container)) {
        VERIFY(container->parent());
        container = *container->parent();
    }

    // 3. Return container.
    return container;
}

// https://dom.spec.whatwg.org/#dom-range-intersectsnode
bool Range::intersects_node(Node const& node) const
{
    // 1. If node’s root is different from this’s root, return false.
    if (&node.root() != &root())
        return false;

    // 2. Let parent be node’s parent.
    auto* parent = node.parent();

    // 3. If parent is null, return true.
    if (!parent)
        return true;

    // 4. Let offset be node’s index.
    auto offset = node.index();

    // 5. If (parent, offset) is before end and (parent, offset plus 1) is after start, return true
    auto relative_position_to_end = position_of_boundary_point_relative_to_other_boundary_point(*parent, offset, m_end_container, m_end_offset);
    auto relative_position_to_start = position_of_boundary_point_relative_to_other_boundary_point(*parent, offset + 1, m_start_container, m_start_offset);
    if (relative_position_to_end == RelativeBoundaryPointPosition::Before && relative_position_to_start == RelativeBoundaryPointPosition::After)
        return true;

    // 6. Return false.
    return false;
}

// https://dom.spec.whatwg.org/#dom-range-ispointinrange
ExceptionOr<bool> Range::is_point_in_range(Node const& node, u32 offset) const
{
    // 1. If node’s root is different from this’s root, return false.
    if (&node.root() != &root())
        return false;

    // 2. If node is a doctype, then throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(node))
        return InvalidNodeTypeError::create("Node cannot be a DocumentType.");

    // 3. If offset is greater than node’s length, then throw an "IndexSizeError" DOMException.
    if (offset > node.length())
        return IndexSizeError::create(String::formatted("Node does not contain a child at offset {}", offset));

    // 4. If (node, offset) is before start or after end, return false.
    auto relative_position_to_start = position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_start_container, m_start_offset);
    auto relative_position_to_end = position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_end_container, m_end_offset);
    if (relative_position_to_start == RelativeBoundaryPointPosition::Before || relative_position_to_end == RelativeBoundaryPointPosition::After)
        return false;

    // 5. Return true.
    return true;
}

// https://dom.spec.whatwg.org/#dom-range-comparepoint
ExceptionOr<i16> Range::compare_point(Node const& node, u32 offset) const
{
    // 1. If node’s root is different from this’s root, then throw a "WrongDocumentError" DOMException.
    if (&node.root() != &root())
        return WrongDocumentError::create("Given node is not in the same document as the range.");

    // 2. If node is a doctype, then throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(node))
        return InvalidNodeTypeError::create("Node cannot be a DocumentType.");

    // 3. If offset is greater than node’s length, then throw an "IndexSizeError" DOMException.
    if (offset > node.length())
        return IndexSizeError::create(String::formatted("Node does not contain a child at offset {}", offset));

    // 4. If (node, offset) is before start, return −1.
    auto relative_position_to_start = position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_start_container, m_start_offset);
    if (relative_position_to_start == RelativeBoundaryPointPosition::Before)
        return -1;

    // 5. If (node, offset) is after end, return 1.
    auto relative_position_to_end = position_of_boundary_point_relative_to_other_boundary_point(node, offset, m_end_container, m_end_offset);
    if (relative_position_to_end == RelativeBoundaryPointPosition::After)
        return 1;

    // 6. Return 0.
    return 0;
}

}
