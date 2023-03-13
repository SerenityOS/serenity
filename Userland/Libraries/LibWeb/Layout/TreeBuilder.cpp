/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/TemporaryChange.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Progress.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>
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

    if (layout_parent.display().is_flex_inside()) {
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

    if (!layout_parent.children_are_inline()) {
        // Parent block has block-level children, insert this block into parent.
        return layout_parent;
    }

    if (layout_node.is_absolutely_positioned() || layout_node.is_floating()) {
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

    auto pseudo_element_style = TRY(style_computer.compute_style(element, pseudo_element));
    auto pseudo_element_content = pseudo_element_style->content();
    auto pseudo_element_display = pseudo_element_style->display();
    // ::before and ::after only exist if they have content. `content: normal` computes to `none` for them.
    // We also don't create them if they are `display: none`.
    if (pseudo_element_display.is_none()
        || pseudo_element_content.type == CSS::ContentData::Type::Normal
        || pseudo_element_content.type == CSS::ContentData::Type::None)
        return {};

    auto pseudo_element_node = DOM::Element::create_layout_node_for_display_type(document, pseudo_element_display, pseudo_element_style, nullptr);
    if (!pseudo_element_node)
        return {};

    pseudo_element_node->set_generated(true);
    // FIXME: Handle images, and multiple values
    if (pseudo_element_content.type == CSS::ContentData::Type::String) {
        auto text = document.heap().allocate<DOM::Text>(document.realm(), document, pseudo_element_content.data.to_deprecated_string()).release_allocated_value_but_fixme_should_propagate_errors();
        auto text_node = document.heap().allocate_without_realm<Layout::TextNode>(document, *text);
        text_node->set_generated(true);
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

ErrorOr<void> TreeBuilder::create_layout_tree(DOM::Node& dom_node, TreeBuilder::Context& context)
{
    // If the parent doesn't have a layout node, we don't need one either.
    if (dom_node.parent_or_shadow_host() && !dom_node.parent_or_shadow_host()->layout_node())
        return {};

    Optional<TemporaryChange<bool>> has_svg_root_change;

    if (dom_node.is_svg_container()) {
        has_svg_root_change.emplace(context.has_svg_root, true);
    } else if (dom_node.requires_svg_container() && !context.has_svg_root) {
        return {};
    }

    auto& document = dom_node.document();
    auto& style_computer = document.style_computer();
    JS::GCPtr<Layout::Node> layout_node;
    RefPtr<CSS::StyleProperties> style;
    CSS::Display display;

    if (is<DOM::Element>(dom_node)) {
        auto& element = static_cast<DOM::Element&>(dom_node);
        element.clear_pseudo_element_nodes({});
        VERIFY(!element.needs_style_update());
        style = element.computed_css_values();
        display = style->display();
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
    } else if (is<DOM::ShadowRoot>(dom_node)) {
        layout_node = document.heap().allocate_without_realm<Layout::BlockContainer>(document, &static_cast<DOM::ShadowRoot&>(dom_node), CSS::ComputedValues {});
        display = CSS::Display(CSS::Display::Outside::Block, CSS::Display::Inside::FlowRoot);
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
        if (shadow_root)
            TRY(create_layout_tree(*shadow_root, context));

        // This is the same as verify_cast<DOM::ParentNode>(dom_node).for_each_child
        for (auto* node = verify_cast<DOM::ParentNode>(dom_node).first_child(); node; node = node->next_sibling())
            TRY(create_layout_tree(*node, context));
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
        auto list_item_marker = document.heap().allocate_without_realm<ListItemMarkerBox>(document, layout_node->computed_values().list_style_type(), child_index + 1, *marker_style);
        static_cast<ListItemBox&>(*layout_node).set_marker(list_item_marker);
        element.set_pseudo_element_node({}, CSS::Selector::PseudoElement::Marker, list_item_marker);
        layout_node->append_child(*list_item_marker);
    }

    if (is<HTML::HTMLProgressElement>(dom_node)) {
        auto& progress = static_cast<HTML::HTMLProgressElement&>(dom_node);
        if (!progress.using_system_appearance()) {
            auto bar_style = TRY(style_computer.compute_style(progress, CSS::Selector::PseudoElement::ProgressBar));
            bar_style->set_property(CSS::PropertyID::Display, CSS::IdentifierStyleValue::create(CSS::ValueID::InlineBlock));
            auto value_style = TRY(style_computer.compute_style(progress, CSS::Selector::PseudoElement::ProgressValue));
            value_style->set_property(CSS::PropertyID::Display, CSS::IdentifierStyleValue::create(CSS::ValueID::Block));
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

    if (is<HTML::HTMLInputElement>(dom_node)) {
        auto& input_element = static_cast<HTML::HTMLInputElement&>(dom_node);

        if (auto placeholder_value = input_element.placeholder_value(); placeholder_value.has_value()) {
            auto placeholder_style = TRY(style_computer.compute_style(input_element, CSS::Selector::PseudoElement::Placeholder));
            auto placeholder = DOM::Element::create_layout_node_for_display_type(document, placeholder_style->display(), placeholder_style, nullptr);

            auto text = document.heap().allocate<DOM::Text>(document.realm(), document, *placeholder_value).release_allocated_value_but_fixme_should_propagate_errors();
            auto text_node = document.heap().allocate_without_realm<Layout::TextNode>(document, *text);
            text_node->set_generated(true);

            push_parent(verify_cast<NodeWithStyle>(*layout_node));
            push_parent(verify_cast<NodeWithStyle>(*placeholder));
            insert_node_into_inline_or_block_ancestor(*text_node, text_node->display(), AppendOrPrepend::Append);
            pop_parent();
            insert_node_into_inline_or_block_ancestor(*placeholder, placeholder->display(), AppendOrPrepend::Append);
            pop_parent();

            input_element.set_pseudo_element_node({}, CSS::Selector::PseudoElement::Placeholder, placeholder);
        }
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
    generate_missing_parents(root);
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
            if (child.display().is_table_column())
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
static void wrap_in_anonymous(Vector<JS::Handle<Node>>& sequence, Node* nearest_sibling)
{
    VERIFY(!sequence.is_empty());
    auto& parent = *sequence.first()->parent();
    auto computed_values = parent.computed_values().clone_inherited_values();
    static_cast<CSS::MutableComputedValues&>(computed_values).set_display(WrapperBoxType::static_display(parent.display().is_inline_outside()));
    auto wrapper = parent.heap().template allocate_without_realm<WrapperBoxType>(parent.document(), nullptr, move(computed_values));
    for (auto& child : sequence) {
        parent.remove_child(*child);
        wrapper->append_child(*child);
    }
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
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });

    // An anonymous table-row box must be generated around each sequence of consecutive children of a table-row-group box which are not table-row boxes.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableRowGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });
    // Unless explicitly mentioned otherwise, mentions of table-row-groups in this spec also encompass the specialized
    // table-header-groups and table-footer-groups.
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableHeaderGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableFooterGroup>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_row, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
        });
    });

    // An anonymous table-cell box must be generated around each sequence of consecutive children of a table-row box which are not table-cell boxes. !Testcase
    for_each_in_tree_with_internal_display<CSS::Display::Internal::TableRow>(root, [&](auto& parent) {
        for_each_sequence_of_consecutive_children_matching(parent, is_not_table_cell, [&](auto& sequence, auto nearest_sibling) {
            wrap_in_anonymous<TableCellBox>(sequence, nearest_sibling);
        });
    });
}

void TreeBuilder::generate_missing_parents(NodeWithStyle& root)
{
    Vector<JS::Handle<TableBox>> table_roots_to_wrap;
    root.for_each_in_inclusive_subtree_of_type<Box>([&](auto& parent) {
        // An anonymous table-row box must be generated around each sequence of consecutive table-cell boxes whose parent is not a table-row.
        if (is_not_table_row(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_table_cell, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<TableRowBox>(sequence, nearest_sibling);
            });
        }

        // A table-row is misparented if its parent is neither a table-row-group nor a table-root box.
        if (!parent.display().is_table_inside() && !is_proper_table_child(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_table_row, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<TableBox>(sequence, nearest_sibling);
            });
        }

        // A table-row-group, table-column-group, or table-caption box is misparented if its parent is not a table-root box.
        if (!parent.display().is_table_inside() && !is_proper_table_child(parent)) {
            for_each_sequence_of_consecutive_children_matching(parent, is_proper_table_child, [&](auto& sequence, auto nearest_sibling) {
                wrap_in_anonymous<TableBox>(sequence, nearest_sibling);
            });
        }

        // An anonymous table-wrapper box must be generated around each table-root.
        if (parent.display().is_table_inside()) {
            table_roots_to_wrap.append(static_cast<TableBox&>(parent));
        }

        return IterationDecision::Continue;
    });

    for (auto& table_box : table_roots_to_wrap) {
        auto* nearest_sibling = table_box->next_sibling();
        auto& parent = *table_box->parent();

        CSS::ComputedValues wrapper_computed_values;
        // The computed values of properties 'position', 'float', 'margin-*', 'top', 'right', 'bottom', and 'left' on the table element are used on the table wrapper box and not the table box;
        // all other values of non-inheritable properties are used on the table box and not the table wrapper box.
        // (Where the table element's values are not used on the table and table wrapper boxes, the initial values are used instead.)
        auto& mutable_wrapper_computed_values = static_cast<CSS::MutableComputedValues&>(wrapper_computed_values);
        if (table_box->display().is_inline_outside())
            mutable_wrapper_computed_values.set_display(CSS::Display::from_short(CSS::Display::Short::InlineBlock));
        else
            mutable_wrapper_computed_values.set_display(CSS::Display::from_short(CSS::Display::Short::FlowRoot));
        mutable_wrapper_computed_values.set_position(table_box->computed_values().position());
        mutable_wrapper_computed_values.set_inset(table_box->computed_values().inset());
        mutable_wrapper_computed_values.set_float(table_box->computed_values().float_());
        mutable_wrapper_computed_values.set_clear(table_box->computed_values().clear());
        mutable_wrapper_computed_values.set_margin(table_box->computed_values().margin());
        table_box->reset_table_box_computed_values_used_by_wrapper_to_init_values();

        auto wrapper = parent.heap().allocate_without_realm<TableWrapper>(parent.document(), nullptr, move(wrapper_computed_values));

        parent.remove_child(*table_box);
        wrapper->append_child(*table_box);

        if (nearest_sibling)
            parent.insert_before(*wrapper, *nearest_sibling);
        else
            parent.append_child(*wrapper);
    }
}

}
