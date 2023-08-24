/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/TemporaryChange.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Progress.h>
#include <LibWeb/Layout/TableGrid.h>
#include <LibWeb/Layout/TableWrapper.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/SVG/SVGForeignObjectElement.h>

namespace Web::Layout {

TreeBuilder::TreeBuilder() = default;

static bool has_inline_or_in_flow_block_children(Layout::Node const& layout_node)
{
    for (auto child = layout_node.first_child(); child; child = child->next_sibling()) {
        if (child->is_inline())
            return true;
        if (!child->is_floating() && !child->is_absolutely_positioned())
            return true;
    }
    return false;
}

static bool has_in_flow_block_children(Layout::Node const& layout_node)
{
    if (layout_node.children_are_inline())
        return false;
    for (auto child = layout_node.first_child(); child; child = child->next_sibling()) {
        if (child->is_inline())
            continue;
        if (!child->is_floating() && !child->is_absolutely_positioned())
            return true;
    }
    return false;
}

// The insertion_parent_for_*() functions maintain the invariant that the in-flow children of
// block-level boxes must be either all block-level or all inline-level.

static Layout::Node& insertion_parent_for_inline_node(Layout::NodeWithStyle& layout_parent)
{
    if (layout_parent.display().is_inline_outside() && layout_parent.display().is_flow_inside())
        return layout_parent;

    if (layout_parent.display().is_flex_inside() || layout_parent.display().is_grid_inside()) {
        layout_parent.append_child(layout_parent.create_anonymous_wrapper());
        return *layout_parent.last_child();
    }

    if (!has_in_flow_block_children(layout_parent) || layout_parent.children_are_inline())
        return layout_parent;

    // Parent has block-level children, insert into an anonymous wrapper block (and create it first if needed)
    if (!layout_parent.last_child()->is_anonymous() || !layout_parent.last_child()->children_are_inline()) {
        layout_parent.append_child(layout_parent.create_anonymous_wrapper());
    }
    return *layout_parent.last_child();
}

static Layout::Node& insertion_parent_for_block_node(Layout::NodeWithStyle& layout_parent, Layout::Node& layout_node)
{
    if (!has_inline_or_in_flow_block_children(layout_parent)) {
        // Parent block has no children, insert this block into parent.
        return layout_parent;
    }

    bool is_out_of_flow = layout_node.is_absolutely_positioned() || layout_node.is_floating();

    if (is_out_of_flow
        && !layout_parent.display().is_flex_inside()
        && !layout_parent.display().is_grid_inside()
        && layout_parent.last_child()->is_anonymous()
        && layout_parent.last_child()->children_are_inline()) {
        // Block is out-of-flow & previous sibling was wrapped in an anonymous block.
        // Join the previous sibling inside the anonymous block.
        return *layout_parent.last_child();
    }

    if (!layout_parent.children_are_inline()) {
        // Parent block has block-level children, insert this block into parent.
        return layout_parent;
    }

    if (is_out_of_flow) {
        // Block is out-of-flow, it can have inline siblings if necessary.
        return layout_parent;
    }

    // Parent block has inline-level children (our siblings).
    // First move these siblings into an anonymous wrapper block.
    Vector<JS::Handle<Layout::Node>> children;
    while (JS::GCPtr<Layout::Node> child = layout_parent.first_child()) {
        layout_parent.remove_child(*child);
        children.append(*child);
    }
    layout_parent.append_child(layout_parent.create_anonymous_wrapper());
    layout_parent.set_children_are_inline(false);
    for (auto& child : children) {
        layout_parent.last_child()->append_child(*child);
    }
    layout_parent.last_child()->set_children_are_inline(true);
    // Then it's safe to insert this block into parent.
    return layout_parent;
}

void TreeBuilder::insert_node_into_inline_or_block_ancestor(Layout::Node& node, CSS::Display display, AppendOrPrepend mode)
{
    if (node.display().is_contents())
        return;

    if (display.is_inline_outside()) {
        // Inlines can be inserted into the nearest ancestor.
        auto& insertion_point = insertion_parent_for_inline_node(m_ancestor_stack.last());
        if (mode == AppendOrPrepend::Prepend)
            insertion_point.prepend_child(node);
        else
            insertion_point.append_child(node);
        insertion_point.set_children_are_inline(true);
    } else {
        // Non-inlines can't be inserted into an inline parent, so find the nearest non-inline ancestor.
        auto& nearest_non_inline_ancestor = [&]() -> Layout::NodeWithStyle& {
            for (auto& ancestor : m_ancestor_stack.in_reverse()) {
                if (ancestor->display().is_contents())
                    continue;
                if (!ancestor->display().is_inline_outside())
                    return ancestor;
                if (!ancestor->display().is_flow_inside())
                    return ancestor;
                if (ancestor->dom_node() && is<SVG::SVGForeignObjectElement>(*ancestor->dom_node()))
                    return ancestor;
            }
            VERIFY_NOT_REACHED();
        }();
        auto& insertion_point = insertion_parent_for_block_node(nearest_non_inline_ancestor, node);
        if (mode == AppendOrPrepend::Prepend)
            insertion_point.prepend_child(node);
        else
            insertion_point.append_child(node);

        // After inserting an in-flow block-level box into a parent, mark the parent as having non-inline children.
        if (!node.is_floating() && !node.is_absolutely_positioned())
            insertion_point.set_children_are_inline(false);
    }
}

ErrorOr<void> TreeBuilder::create_pseudo_element_if_needed(DOM::Element& element, CSS::Selector::PseudoElement pseudo_element, AppendOrPrepend mode)
{
    auto& document = element.document();
    auto& style_computer = document.style_computer();

    auto pseudo_element_style = TRY(style_computer.compute_pseudo_element_style_if_needed(element, pseudo_element));
    if (!pseudo_element_style)
        return {};

    auto pseudo_element_content = pseudo_element_style->content();
    auto pseudo_element_display = pseudo_element_style->display();
    // ::before and ::after only exist if they have content. `content: normal` computes to `none` for them.
    // We also don't create them if they are `display: none`.
    if (pseudo_element_display.is_none()
        || pseudo_element_content.type == CSS::ContentData::Type::Normal
        || pseudo_element_content.type == CSS::ContentData::Type::None)
        return {};

    auto pseudo_element_node = DOM::Element::create_layout_node_for_display_type(document, pseudo_element_display, *pseudo_element_style, nullptr);
    if (!pseudo_element_node)
        return {};

    auto generated_for = Node::GeneratedFor::NotGenerated;
    if (pseudo_element == CSS::Selector::PseudoElement::Before) {
        generated_for = Node::GeneratedFor::PseudoBefore;
    } else if (pseudo_element == CSS::Selector::PseudoElement::After) {
        generated_for = Node::GeneratedFor::PseudoAfter;
    } else {
        VERIFY_NOT_REACHED();
    }

    pseudo_element_node->set_generated_for(generated_for, element);

    // FIXME: Handle images, and multiple values
    if (pseudo_element_content.type == CSS::ContentData::Type::String) {
        auto text = document.heap().allocate<DOM::Text>(document.realm(), document, pseudo_element_content.data.to_deprecated_string());
        auto text_node = document.heap().allocate_without_realm<Layout::TextNode>(document, *text);
        text_node->set_generated_for(generated_for, element);

        push_parent(verify_cast<NodeWithStyle>(*pseudo_element_node));
        insert_node_into_inline_or_block_ancestor(*text_node, text_node->display(), AppendOrPrepend::Append);
        pop_parent();
    } else {
        TODO();
    }

    element.set_pseudo_element_node({}, pseudo_element, pseudo_element_node);
    insert_node_into_inline_or_block_ancestor(*pseudo_element_node, pseudo_element_display, mode);

    return {};
}

static bool is_ignorable_whitespace(Layout::Node const& node)
{
    if (node.is_text_node() && static_cast<TextNode const&>(node).text_for_rendering().is_whitespace())
        return true;

    if (node.is_anonymous() && node.is_block_container() && static_cast<BlockContainer const&>(node).children_are_inline()) {
        bool contains_only_white_space = true;
        node.for_each_in_inclusive_subtree_of_type<TextNode>([&contains_only_white_space](auto& text_node) {
            if (!text_node.text_for_rendering().is_whitespace()) {
                contains_only_white_space = false;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (contains_only_white_space)
            return true;
    }

    return false;
}

ErrorOr<void> TreeBuilder::create_layout_tree(DOM::Node& dom_node, TreeBuilder::Context& context)
{
    JS::GCPtr<Layout::Node> layout_node;
    Optional<TemporaryChange<bool>> has_svg_root_change;

    ScopeGuard remove_stale_layout_node_guard = [&] {
        // If we didn't create a layout node for this DOM node,
        // go through the DOM tree and remove any old layout nodes since they are now all stale.
        if (!layout_node) {
            dom_node.for_each_in_inclusive_subtree([&](auto& node) {
                node.detach_layout_node({});
                if (is<DOM::Element>(node))
                    static_cast<DOM::Element&>(node).clear_pseudo_element_nodes({});
                return IterationDecision::Continue;
            });
        }
    };

    if (dom_node.is_svg_container()) {
        has_svg_root_change.emplace(context.has_svg_root, true);
    } else if (dom_node.requires_svg_container() && !context.has_svg_root) {
        return {};
    }

    auto& document = dom_node.document();
    auto& style_computer = document.style_computer();
    RefPtr<CSS::StyleProperties> style;
    CSS::Display display;

    if (is<DOM::Element>(dom_node)) {
        auto& element = static_cast<DOM::Element&>(dom_node);

        // Special path for ::placeholder, which corresponds to a synthetic DOM element inside the <input> UA shadow root.
        // FIXME: This is very hackish. Find a better way to architect this.
        if (element.pseudo_element() == CSS::Selector::PseudoElement::Placeholder) {
            auto& input_element = verify_cast<HTML::HTMLInputElement>(*element.root().parent_or_shadow_host());
            style = TRY(style_computer.compute_style(input_element, CSS::Selector::PseudoElement::Placeholder));
            if (input_element.placeholder_value().has_value())
                display = style->display();
            else
                display = CSS::Display::from_short(CSS::Display::Short::None);
        }
        // Common path: this is a regular DOM element. Style should be present already, thanks to Document::update_style().
        else {
            element.clear_pseudo_element_nodes({});
            VERIFY(!element.needs_style_update());
            style = element.computed_css_values();
            display = style->display();
        }
        if (display.is_none())
            return {};
        layout_node = element.create_layout_node(*style);
    } else if (is<DOM::Document>(dom_node)) {
        style = style_computer.create_document_style();
        display = style->display();
        layout_node = document.heap().allocate_without_realm<Layout::Viewport>(static_cast<DOM::Document&>(dom_node), *style);
    } else if (is<DOM::Text>(dom_node)) {
        layout_node = document.heap().allocate_without_realm<Layout::TextNode>(document, static_cast<DOM::Text&>(dom_node));
        display = CSS::Display(CSS::Display::Outside::Inline, CSS::Display::Inside::Flow);
    }

    if (!layout_node)
        return {};

    if (!dom_node.parent_or_shadow_host()) {
        m_layout_root = layout_node;
    } else if (layout_node->is_svg_box()) {
        m_ancestor_stack.last()->append_child(*layout_node);
    } else {
        insert_node_into_inline_or_block_ancestor(*layout_node, display, AppendOrPrepend::Append);
    }

    auto* shadow_root = is<DOM::Element>(dom_node) ? verify_cast<DOM::Element>(dom_node).shadow_root_internal() : nullptr;

    if ((dom_node.has_children() || shadow_root) && layout_node->can_have_children()) {
        push_parent(verify_cast<NodeWithStyle>(*layout_node));
        if (shadow_root) {
            for (auto* node = shadow_root->first_child(); node; node = node->next_sibling()) {
                TRY(create_layout_tree(*node, context));
            }
        } else {
            // This is the same as verify_cast<DOM::ParentNode>(dom_node).for_each_child
            for (auto* node = verify_cast<DOM::ParentNode>(dom_node).first_child(); node; node = node->next_sibling())
                TRY(create_layout_tree(*node, context));
        }
        pop_parent();
    }

    // Add nodes for the ::before and ::after pseudo-elements.
    if (is<DOM::Element>(dom_node)) {
        auto& element = static_cast<DOM::Element&>(dom_node);
        push_parent(verify_cast<NodeWithStyle>(*layout_node));
        TRY(create_pseudo_element_if_needed(element, CSS::Selector::PseudoElement::Before, AppendOrPrepend::Prepend));
        TRY(create_pseudo_element_if_needed(element, CSS::Selector::PseudoElement::After, AppendOrPrepend::Append));
        pop_parent();
    }

    if (is<ListItemBox>(*layout_node)) {
        auto& element = static_cast<DOM::Element&>(dom_node);
        int child_index = layout_node->parent()->index_of_child<ListItemBox>(*layout_node).value();
        auto marker_style = TRY(style_computer.compute_style(element, CSS::Selector::PseudoElement::Marker));
        auto list_item_marker = document.heap().allocate_without_realm<ListItemMarkerBox>(document, layout_node->computed_values().list_style_type(), layout_node->computed_values().list_style_position(), child_index + 1, *marker_style);
        static_cast<ListItemBox&>(*layout_node).set_marker(list_item_marker);
        element.set_pseudo_element_node({}, CSS::Selector::PseudoElement::Marker, list_item_marker);
        layout_node->append_child(*list_item_marker);
    }

    if (is<HTML::HTMLProgressElement>(dom_node)) {
        auto& progress = static_cast<HTML::HTMLProgressElement&>(dom_node);
        if (!progress.using_system_appearance()) {
            auto bar_style = TRY(style_computer.compute_style(progress, CSS::Selector::PseudoElement::ProgressBar));
            bar_style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::FlowRoot)));
            auto value_style = TRY(style_computer.compute_style(progress, CSS::Selector::PseudoElement::ProgressValue));
            value_style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Block)));
            auto position = progress.position();
            value_style->set_property(CSS::PropertyID::Width, CSS::PercentageStyleValue::create(CSS::Percentage(position >= 0 ? round_to<int>(100 * position) : 0)));
            auto bar_display = bar_style->display();
            auto value_display = value_style->display();
            auto progress_bar = DOM::Element::create_layout_node_for_display_type(document, bar_display, bar_style, nullptr);
            auto progress_value = DOM::Element::create_layout_node_for_display_type(document, value_display, value_style, nullptr);
            push_parent(verify_cast<NodeWithStyle>(*layout_node));
            push_parent(verify_cast<NodeWithStyle>(*progress_bar));
            insert_node_into_inline_or_block_ancestor(*progress_value, value_display, AppendOrPrepend::Append);
            pop_parent();
            insert_node_into_inline_or_block_ancestor(*progress_bar, bar_display, AppendOrPrepend::Append);
            pop_parent();
            progress.set_pseudo_element_node({}, CSS::Selector::PseudoElement::ProgressBar, progress_bar);
            progress.set_pseudo_element_node({}, CSS::Selector::PseudoElement::ProgressValue, progress_value);
        }
    }

    // https://html.spec.whatwg.org/multipage/rendering.html#button-layout
    // If the element is an input element, or if it is a button element and its computed value for
    // 'display' is not 'inline-grid', 'grid', 'inline-flex', or 'flex', then the element's box has
    // a child anonymous button content box with the following behaviors:
    if (is<HTML::HTMLButtonElement>(dom_node) && !display.is_grid_inside() && !display.is_flex_inside()) {
        auto& parent = *dom_node.layout_node();

        // If the box does not overflow in the vertical axis, then it is centered vertically.
        auto table_computed_values = parent.computed_values().clone_inherited_values();
        static_cast<CSS::MutableComputedValues&>(table_computed_values).set_display(CSS::Display::from_short(CSS::Display::Short::Table));
        static_cast<CSS::MutableComputedValues&>(table_computed_values).set_height(CSS::Size::make_percentage(CSS::Percentage(100)));

        auto cell_computed_values = parent.computed_values().clone_inherited_values();
        static_cast<CSS::MutableComputedValues&>(cell_computed_values).set_display(CSS::Display { CSS::Display::Internal::TableCell });
        static_cast<CSS::MutableComputedValues&>(cell_computed_values).set_vertical_align(CSS::VerticalAlign::Middle);

        auto row_computed_values = parent.computed_values().clone_inherited_values();
        static_cast<CSS::MutableComputedValues&>(row_computed_values).set_display(CSS::Display { CSS::Display::Internal::TableRow });

        auto table_wrapper = parent.heap().template allocate_without_realm<BlockContainer>(parent.document(), nullptr, move(table_computed_values));
        auto cell_wrapper = parent.heap().template allocate_without_realm<BlockContainer>(parent.document(), nullptr, move(cell_computed_values));
        auto row_wrapper = parent.heap().template allocate_without_realm<Box>(parent.document(), nullptr, move(row_computed_values));

        cell_wrapper->set_line_height(parent.line_height());
        cell_wrapper->set_font(parent.font());
        cell_wrapper->set_children_are_inline(parent.children_are_inline());
        row_wrapper->set_children_are_inline(false);

        Vector<JS::Handle<Node>> sequence;
        for (auto child = parent.first_child(); child; child = child->next_sibling()) {
            sequence.append(*child);
        }

        for (auto& node : sequence) {
            parent.remove_child(*node);
            cell_wrapper->append_child(*node);
        }

        row_wrapper->append_child(*cell_wrapper);
        table_wrapper->append_child(*row_wrapper);
        parent.append_child(*table_wrapper);
    }

    return {};
}

JS::GCPtr<Layout::Node> TreeBuilder::build(DOM::Node& dom_node)
{
    VERIFY(dom_node.is_document());

    Context context;
    MUST(create_layout_tree(dom_node, context)); // FIXME propagate errors

    if (auto* root = dom_node.document().layout_node())
        fixup_tables(*root);

    return move(m_layout_root);
}

template<CSS::Display::Internal internal, typename Callback>
void TreeBuilder::for_each_in_tree_with_internal_display(NodeWithStyle& root, Callback callback)
{
    root.for_each_in_inclusive_subtree_of_type<Box>([&](auto& box) {
        auto const display = box.display();
        if (display.is_internal() && display.internal() == internal)
            callback(box);
        return IterationDecision::Continue;
    });
}

template<CSS::Display::Inside inside, typename Callback>
void TreeBuilder::for_each_in_tree_with_inside_display(NodeWithStyle& root, Callback callback)
{
    root.for_each_in_inclusive_subtree_of_type<Box>([&](auto& box) {
        auto const display = box.display();
        if (display.is_outside_and_inside() && display.inside() == inside)
            callback(box);
        return IterationDecision::Continue;
    });
}

void TreeBuilder::fixup_tables(NodeWithStyle& root)
{
    remove_irrelevant_boxes(root);
    generate_missing_child_wrappers(root);
    auto table_root_boxes = generate_missing_parents(root);
    missing_cells_fixup(table_root_boxes);
}

void TreeBuilder::remove_irrelevant_boxes(NodeWithStyle& root)
{
    // The following boxes are discarded as if they were display:none:

    Vector<JS::Handle<Node>> to_remove;

    // Children of a table-column.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableColumn>(root, [&](Box& table_column) {
        table_column.for_each_child([&](auto& child) {
            to_remove.append(child);
        });
    });

    // Children of a table-column-group which are not a table-column.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableColumnGroup>(root, [&](Box& table_column_group) {
        table_column_group.for_each_child([&](auto& child) {
            if (!child.display().is_table_column())
                to_remove.append(child);
        });
    });

    // FIXME:
    // Anonymous inline boxes which contain only white space and are between two immediate siblings each of which is a table-non-root box.
    // Anonymous inline boxes which meet all of the following criteria:
    // - they contain only white space
    // - they are the first and/or last child of a tabular container
    // - whose immediate sibling, if any, is a table-non-root box

    for (auto& box : to_remove)
        box->parent()->remove_child(*box);
}

static bool is_table_track(CSS::Display display)
{
    return display.is_table_row() || display.is_table_column();
}

static bool is_table_track_group(CSS::Display display)
{
    // Unless explicitly mentioned otherwise, mentions of table-row-groups in this spec also encompass the specialized
    // table-header-groups and table-footer-groups.
    return display.is_table_row_group()
        || display.is_table_header_group()
        || display.is_table_footer_group()
        || display.is_table_column_group();
}

static bool is_proper_table_child(Node const& node)
{
    auto const display = node.display();
    return is_table_track_group(display) || is_table_track(display) || display.is_table_caption();
}

static bool is_not_proper_table_child(Node const& node)
{
    if (!node.has_style())
        return true;
    return !is_proper_table_child(node);
}

static bool is_table_row(Node const& node)
{
    return node.display().is_table_row();
}

static bool is_not_table_row(Node const& node)
{
    if (!node.has_style())
        return true;
    return !is_table_row(node);
}

static bool is_table_cell(Node const& node)
{
    return node.display().is_table_cell();
}

static bool is_not_table_cell(Node const& node)
{
    if (!node.has_style())
        return true;
    return !is_table_cell(node);
}

template<typename Matcher, typename Callback>
static void for_each_sequence_of_consecutive_children_matching(NodeWithStyle& parent, Matcher matcher, Callback callback)
{
    Vector<JS::Handle<Node>> sequence;

    auto sequence_is_all_ignorable_whitespace = [&]() -> bool {
        for (auto& node : sequence) {
            if (!is_ignorable_whitespace(*node))
                return false;
        }
        return true;
    };

    for (auto child = parent.first_child(); child; child = child->next_sibling()) {
        if (matcher(*child) || (!sequence.is_empty() && is_ignorable_whitespace(*child))) {
            sequence.append(*child);
        } else {
            if (!sequence.is_empty()) {
                if (!sequence_is_all_ignorable_whitespace())
                    callback(sequence, child);
                sequence.clear();
            }
        }
    }
    if (!sequence.is_empty() && !sequence_is_all_ignorable_whitespace())
        callback(sequence, nullptr);
}

template<typename WrapperBoxType>
static void wrap_in_anonymous(Vector<JS::Handle<Node>>& sequence, Node* nearest_sibling, CSS::Display display)
{
    VERIFY(!sequence.is_empty());
    auto& parent = *sequence.first()->parent();
    auto computed_values = parent.computed_values().clone_inherited_values();
    static_cast<CSS::MutableComputedValues&>(computed_values).set_display(display);
    auto wrapper = parent.heap().template allocate_without_realm<WrapperBoxType>(parent.document(), nullptr, move(computed_values));
    for (auto& child : sequence) {
        parent.remove_child(*child);
        wrapper->append_child(*child);
    }
    wrapper->set_children_are_inline(parent.children_are_inline());
    if (nearest_sibling)
        parent.insert_before(*wrapper, *nearest_sibling);
    else
        parent.append_child(*wrapper);
}

void TreeBuilder::generate_missing_child_wrappers(NodeWithStyle& root)
{
    // An anonymous table-row box must be generated around each sequence of consecutive children of a table-root box which are not proper table child boxes.
    for_each_in_tree_with_inside_display<CSS::Display::Inside::Table>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_proper_table_child, [&](auto sequence, auto nearest_sibling) {
            wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableRow });
        });
    });

    // An anonymous table-row box must be generated around each sequence of consecutive children of a table-row-group box which are not table-row boxes.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableRowGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableRow });
        });
    });
    // Unless explicitly mentioned otherwise, mentions of table-row-groups in this spec also encompass the specialized
    // table-header-groups and table-footer-groups.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableHeaderGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableRow });
        });
    });
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableFooterGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableRow });
        });
    });

    // An anonymous table-cell box must be generated around each sequence of consecutive children of a table-row box which are not table-cell boxes. !Testcase
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableRow>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_cell, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<BlockContainer>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableCell });
        });
    });
}

Vector<JS::Handle<Box>> TreeBuilder::generate_missing_parents(NodeWithStyle& root)
{
    Vector<JS::Handle<Box>> table_roots_to_wrap;
    root.for_each_in_inclusive_subtree_of_type<Box>([&](auto& parent) {
        // An anonymous table-row box must be generated around each sequence of consecutive table-cell boxes whose parent is not a table-row.
        if (is_not_table_row(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_table_cell, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display { CSS::Display::Internal::TableRow });
            });
        }

        // A table-row is misparented if its parent is neither a table-row-group nor a table-root box.
        if (!parent.display().is_table_inside() && !is_proper_table_child(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_table_row, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display::from_short(parent.display().is_inline_outside() ? CSS::Display::Short::InlineTable : CSS::Display::Short::Table));
            });
        }

        // A table-row-group, table-column-group, or table-caption box is misparented if its parent is not a table-root box.
        if (!parent.display().is_table_inside() && !is_proper_table_child(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_proper_table_child, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<Box>(sequence, nearest_sibling, CSS::Display::from_short(parent.display().is_inline_outside() ? CSS::Display::Short::InlineTable : CSS::Display::Short::Table));
            });
        }

        // An anonymous table-wrapper box must be generated around each table-root.
        if (parent.display().is_table_inside()) {
            table_roots_to_wrap.append(parent);
        }

        return IterationDecision::Continue;
    });

    for (auto& table_box : table_roots_to_wrap) {
        auto* nearest_sibling = table_box->next_sibling();
        auto& parent = *table_box->parent();

        CSS::ComputedValues wrapper_computed_values;
        table_box->transfer_table_box_computed_values_to_wrapper_computed_values(wrapper_computed_values);

        auto wrapper = parent.heap().allocate_without_realm<TableWrapper>(parent.document(), nullptr, move(wrapper_computed_values));

        parent.remove_child(*table_box);
        wrapper->append_child(*table_box);

        if (nearest_sibling)
            parent.insert_before(*wrapper, *nearest_sibling);
        else
            parent.append_child(*wrapper);
    }

    return table_roots_to_wrap;
}

template<typename Matcher, typename Callback>
static void for_each_child_box_matching(Box& parent, Matcher matcher, Callback callback)
{
    parent.for_each_child_of_type<Box>([&](Box& child_box) {
        if (matcher(child_box))
            callback(child_box);
    });
}

static void fixup_row(Box& row_box, TableGrid const& table_grid, size_t row_index)
{
    bool missing_cells_run_has_started = false;
    for (size_t column_index = 0; column_index < table_grid.column_count(); ++column_index) {
        if (table_grid.occupancy_grid().contains({ column_index, row_index })) {
            VERIFY(!missing_cells_run_has_started);
            continue;
        }
        missing_cells_run_has_started = true;
        auto row_computed_values = row_box.computed_values().clone_inherited_values();
        auto& cell_computed_values = static_cast<CSS::MutableComputedValues&>(row_computed_values);
        cell_computed_values.set_display(Web::CSS::Display { CSS::Display::Internal::TableCell });
        // Ensure that the cell (with zero content height) will have the same height as the row by setting vertical-align to middle.
        cell_computed_values.set_vertical_align(CSS::VerticalAlign::Middle);
        auto cell_box = row_box.heap().template allocate_without_realm<BlockContainer>(row_box.document(), nullptr, cell_computed_values);
        row_box.append_child(cell_box);
    }
}

void TreeBuilder::missing_cells_fixup(Vector<JS::Handle<Box>> const& table_root_boxes)
{
    // Implements https://www.w3.org/TR/css-tables-3/#missing-cells-fixup.
    for (auto& table_box : table_root_boxes) {
        auto table_grid = TableGrid::calculate_row_column_grid(*table_box);
        size_t row_index = 0;
        for_each_child_box_matching(*table_box, TableGrid::is_table_row_group, [&](auto& row_group_box) {
            for_each_child_box_matching(row_group_box, is_table_row, [&](auto& row_box) {
                fixup_row(row_box, table_grid, row_index);
                ++row_index;
                return IterationDecision::Continue;
            });
        });

        for_each_child_box_matching(*table_box, is_table_row, [&](auto& row_box) {
            fixup_row(row_box, table_grid, row_index);
            ++row_index;
            return IterationDecision::Continue;
        });
    }
}
}
