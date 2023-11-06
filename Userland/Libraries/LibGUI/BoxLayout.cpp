/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Frhun <serenitystuff@frhun.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Margins.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Orientation.h>
#include <stdio.h>

REGISTER_LAYOUT(GUI, HorizontalBoxLayout)
REGISTER_LAYOUT(GUI, VerticalBoxLayout)

namespace GUI {

BoxLayout::BoxLayout(Orientation orientation, Margins margins, int spacing)
    : Layout(margins, spacing)
    , m_orientation(orientation)
{
    register_property(
        "orientation"sv, [this] { return m_orientation == Gfx::Orientation::Vertical ? "Vertical" : "Horizontal"; }, nullptr, nullptr);
}

UISize BoxLayout::preferred_size() const
{
    VERIFY(m_owner);

    UIDimension result_primary { 0 };
    UIDimension result_secondary { 0 };

    bool first_item { true };
    for (auto& entry : m_entries) {
        if (!entry.widget || !entry.widget->is_visible())
            continue;

        UISize min_size = entry.widget->effective_min_size();
        UISize max_size = entry.widget->max_size();
        UISize preferred_size = entry.widget->effective_preferred_size();

        if (result_primary != SpecialDimension::Grow) {
            UIDimension item_primary_size = clamp(
                preferred_size.primary_size_for_orientation(orientation()),
                min_size.primary_size_for_orientation(orientation()),
                max_size.primary_size_for_orientation(orientation()));

            if (item_primary_size.is_int())
                result_primary.add_if_int(item_primary_size.as_int());

            if (item_primary_size.is_grow())
                result_primary = SpecialDimension::Grow;

            if (!first_item)
                result_primary.add_if_int(spacing());
        }

        {
            UIDimension secondary_preferred_size = preferred_size.secondary_size_for_orientation(orientation());

            if (secondary_preferred_size == SpecialDimension::OpportunisticGrow)
                secondary_preferred_size = 0;

            UIDimension item_secondary_size = clamp(
                secondary_preferred_size,
                min_size.secondary_size_for_orientation(orientation()),
                max_size.secondary_size_for_orientation(orientation()));

            result_secondary = max(item_secondary_size, result_secondary);
        }

        first_item = false;
    }

    result_primary.add_if_int(
        margins().primary_total_for_orientation(orientation())
        + m_owner->content_margins().primary_total_for_orientation(orientation()));

    result_secondary.add_if_int(
        margins().secondary_total_for_orientation(orientation())
        + m_owner->content_margins().secondary_total_for_orientation(orientation()));

    if (orientation() == Gfx::Orientation::Horizontal)
        return { result_primary, result_secondary };
    return { result_secondary, result_primary };
}

UISize BoxLayout::min_size() const
{
    VERIFY(m_owner);

    UIDimension result_primary { 0 };
    UIDimension result_secondary { 0 };

    bool first_item { true };
    for (auto& entry : m_entries) {
        if (!entry.widget || !entry.widget->is_visible())
            continue;

        UISize min_size = entry.widget->effective_min_size();

        {
            UIDimension primary_min_size = min_size.primary_size_for_orientation(orientation());

            VERIFY(primary_min_size.is_one_of(SpecialDimension::Shrink, SpecialDimension::Regular));

            if (primary_min_size.is_int())
                result_primary.add_if_int(primary_min_size.as_int());

            if (!first_item)
                result_primary.add_if_int(spacing());
        }

        {
            UIDimension secondary_min_size = min_size.secondary_size_for_orientation(orientation());

            VERIFY(secondary_min_size.is_one_of(SpecialDimension::Shrink, SpecialDimension::Regular));

            result_secondary = max(result_secondary, secondary_min_size);
        }

        first_item = false;
    }

    result_primary.add_if_int(
        margins().primary_total_for_orientation(orientation())
        + m_owner->content_margins().primary_total_for_orientation(orientation()));

    result_secondary.add_if_int(
        margins().secondary_total_for_orientation(orientation())
        + m_owner->content_margins().secondary_total_for_orientation(orientation()));

    if (orientation() == Gfx::Orientation::Horizontal)
        return { result_primary, result_secondary };
    return { result_secondary, result_primary };
}

void BoxLayout::run(Widget& widget)
{
    if (m_entries.is_empty())
        return;

    struct Item {
        Widget* widget { nullptr };
        UIDimension min_size { SpecialDimension::Shrink };
        UIDimension max_size { SpecialDimension::Grow };
        UIDimension preferred_size { SpecialDimension::Shrink };
        int size { 0 };
        bool final { false };
    };

    Vector<Item, 32> items;
    int spacer_count = 0;
    int opportunistic_growth_item_count = 0;
    int opportunistic_growth_items_base_size_total = 0;

    for (size_t i = 0; i < m_entries.size(); ++i) {
        auto& entry = m_entries[i];
        if (entry.type == Entry::Type::Spacer) {
            items.append(Item { nullptr, { SpecialDimension::Shrink }, { SpecialDimension::Grow }, { SpecialDimension::Grow } });
            spacer_count++;
            continue;
        }
        if (!entry.widget)
            continue;
        if (!entry.widget->is_visible())
            continue;
        auto min_size = entry.widget->effective_min_size().primary_size_for_orientation(orientation());
        auto max_size = entry.widget->max_size().primary_size_for_orientation(orientation());
        auto preferred_size = entry.widget->effective_preferred_size().primary_size_for_orientation(orientation());

        if (preferred_size == SpecialDimension::OpportunisticGrow) {
            opportunistic_growth_item_count++;
            opportunistic_growth_items_base_size_total += MUST(min_size.shrink_value());
        } else {
            preferred_size = clamp(preferred_size, min_size, max_size);
        }

        items.append(
            Item {
                entry.widget.ptr(),
                min_size,
                max_size,
                preferred_size });
    }

    if (items.is_empty())
        return;

    Gfx::IntRect content_rect = widget.content_rect();
    int uncommitted_size = content_rect.size().primary_size_for_orientation(orientation())
        - spacing() * (items.size() - 1 - spacer_count)
        - margins().primary_total_for_orientation(orientation());
    int unfinished_regular_items = items.size() - spacer_count - opportunistic_growth_item_count;
    int max_amongst_the_min_sizes = 0;
    int max_amongst_the_min_sizes_of_opportunistically_growing_items = 0;
    int regular_items_to_layout = 0;
    int regular_items_min_size_total = 0;

    // Pass 1: Set all items to their minimum size.
    for (auto& item : items) {
        VERIFY(item.min_size.is_one_of(SpecialDimension::Regular, SpecialDimension::Shrink));
        item.size = MUST(item.min_size.shrink_value());
        uncommitted_size -= item.size;

        if (item.min_size.is_int() && item.max_size.is_int() && item.min_size == item.max_size) {
            // Fixed-size items finish immediately in the first pass.
            item.final = true;
            if (item.preferred_size == SpecialDimension::OpportunisticGrow) {
                opportunistic_growth_item_count--;
                opportunistic_growth_items_base_size_total -= MUST(item.min_size.shrink_value());
            } else {
                --unfinished_regular_items;
            }
        } else if (item.preferred_size != SpecialDimension::OpportunisticGrow && item.widget) {
            max_amongst_the_min_sizes = max(max_amongst_the_min_sizes, MUST(item.min_size.shrink_value()));
            regular_items_to_layout++;
            regular_items_min_size_total += item.size;
        } else if (item.preferred_size == SpecialDimension::OpportunisticGrow) {
            max_amongst_the_min_sizes_of_opportunistically_growing_items = max(max_amongst_the_min_sizes_of_opportunistically_growing_items, MUST(item.min_size.shrink_value()));
        }
    }

    // Pass 2: Set all non final, non spacer items to the previously encountered maximum min_size of these kind of items
    // This is done to ensure even growth, if the items don't have the same min_size, which most won't have.
    // If you are unsure what effect this has, try looking at widget gallery with, and without this, it'll be obvious.
    if (uncommitted_size > 0) {
        int total_growth_if_not_overcommitted = regular_items_to_layout * max_amongst_the_min_sizes - regular_items_min_size_total;
        int overcommitment_if_all_same_min_size = total_growth_if_not_overcommitted - uncommitted_size;
        for (auto& item : items) {
            if (item.final || item.preferred_size == SpecialDimension::OpportunisticGrow || !item.widget)
                continue;
            int extra_needed_space = max_amongst_the_min_sizes - item.size;

            if (overcommitment_if_all_same_min_size > 0) {
                extra_needed_space -= (overcommitment_if_all_same_min_size * extra_needed_space + (total_growth_if_not_overcommitted - 1)) / (total_growth_if_not_overcommitted);
            }

            VERIFY(extra_needed_space >= 0);
            VERIFY(uncommitted_size >= extra_needed_space);

            item.size += extra_needed_space;
            if (item.max_size.is_int() && item.size > item.max_size.as_int())
                item.size = item.max_size.as_int();
            uncommitted_size -= item.size - MUST(item.min_size.shrink_value());
        }
    }

    // Pass 3: Determine final item size for non spacers, and non opportunisticially growing widgets
    int loop_counter = 0; // This doubles as a safeguard for when the loop below doesn't finish for some reason, and as a mechanism to ensure it runs at least once.
    // This has to run at least once, to handle the case where the loop for evening out the min sizes was in an overcommitted state,
    // and gave the Widget a larger size than its preferred size.
    while (unfinished_regular_items && (uncommitted_size > 0 || loop_counter++ == 0)) {
        VERIFY(loop_counter < 100);
        int slice = uncommitted_size / unfinished_regular_items;
        // If uncommitted_size does not divide evenly by unfinished_regular_items,
        // there are some extra pixels that have to be distributed.
        int pixels = uncommitted_size - slice * unfinished_regular_items;
        uncommitted_size = 0;

        for (auto& item : items) {
            if (item.final)
                continue;
            if (!item.widget)
                continue;
            if (item.preferred_size == SpecialDimension::OpportunisticGrow)
                continue;

            int pixel = pixels ? 1 : 0;
            pixels -= pixel;
            int item_size_with_full_slice = item.size + slice + pixel;

            UIDimension resulting_size { 0 };
            resulting_size = max(item.size, item_size_with_full_slice);
            resulting_size = min(resulting_size, item.preferred_size);
            resulting_size = min(resulting_size, item.max_size);

            if (resulting_size.is_shrink()) {
                // FIXME: Propagate this error, so it is obvious where the mistake is actually made.
                if (!item.min_size.is_int())
                    dbgln("BoxLayout: underconstrained widget set to zero size: {} {}", item.widget->class_name(), item.widget->name());
                resulting_size = MUST(item.min_size.shrink_value());
                item.final = true;
            }

            if (resulting_size.is_grow())
                resulting_size = item_size_with_full_slice;

            item.size = resulting_size.as_int();

            // If the slice was more than we needed, return remainder to available_size.
            // Note that this will in some cases even return more than the slice size.
            uncommitted_size += item_size_with_full_slice - item.size;

            if (item.final
                || (item.max_size.is_int() && item.max_size.as_int() == item.size)
                || (item.preferred_size.is_int() && item.preferred_size.as_int() == item.size)) {
                item.final = true;
                --unfinished_regular_items;
            }
        }
    }

    // Pass 4: Even out min_size for opportunistically growing items, analogous to pass 2
    if (uncommitted_size > 0 && opportunistic_growth_item_count > 0) {
        int total_growth_if_not_overcommitted = opportunistic_growth_item_count * max_amongst_the_min_sizes_of_opportunistically_growing_items - opportunistic_growth_items_base_size_total;
        int overcommitment_if_all_same_min_size = total_growth_if_not_overcommitted - uncommitted_size;
        for (auto& item : items) {
            if (item.final || item.preferred_size != SpecialDimension::OpportunisticGrow || !item.widget)
                continue;
            int extra_needed_space = max_amongst_the_min_sizes_of_opportunistically_growing_items - item.size;

            if (overcommitment_if_all_same_min_size > 0 && total_growth_if_not_overcommitted > 0) {
                extra_needed_space -= (overcommitment_if_all_same_min_size * extra_needed_space + (total_growth_if_not_overcommitted - 1)) / (total_growth_if_not_overcommitted);
            }

            VERIFY(extra_needed_space >= 0);
            VERIFY(uncommitted_size >= extra_needed_space);

            item.size += extra_needed_space;
            if (item.max_size.is_int() && item.size > item.max_size.as_int())
                item.size = item.max_size.as_int();
            uncommitted_size -= item.size - MUST(item.min_size.shrink_value());
        }
    }

    loop_counter = 0;
    // Pass 5: Determine the size for the opportunistically growing items.
    while (opportunistic_growth_item_count > 0 && uncommitted_size > 0) {
        VERIFY(loop_counter++ < 200);
        int opportunistic_growth_item_extra_size = uncommitted_size / opportunistic_growth_item_count;
        int pixels = uncommitted_size - opportunistic_growth_item_count * opportunistic_growth_item_extra_size;
        VERIFY(pixels >= 0);
        for (auto& item : items) {
            if (item.preferred_size != SpecialDimension::OpportunisticGrow || item.final || !item.widget)
                continue;

            int pixel = (pixels > 0 ? 1 : 0);
            pixels -= pixel;
            int previous_size = item.size;
            item.size += opportunistic_growth_item_extra_size + pixel;
            if (item.max_size.is_int() && item.size >= item.max_size.as_int()) {
                item.size = item.max_size.as_int();
                item.final = true;
                opportunistic_growth_item_count--;
            }
            uncommitted_size -= item.size - previous_size;
        }
    }

    // Determine size of the spacers, according to the still uncommitted size
    int spacer_width = 0;
    if (spacer_count > 0 && uncommitted_size > 0) {
        spacer_width = uncommitted_size / spacer_count;
    }

    // Pass 6: Place the widgets.
    int current_x = margins().left() + content_rect.x();
    int current_y = margins().top() + content_rect.y();

    auto widget_rect_with_margins_subtracted = margins().applied_to(content_rect);

    for (auto& item : items) {
        Gfx::IntRect rect { current_x, current_y, 0, 0 };

        rect.set_primary_size_for_orientation(orientation(), item.size);

        if (item.widget) {
            int secondary = widget.content_size().secondary_size_for_orientation(orientation());
            secondary -= margins().secondary_total_for_orientation(orientation());

            UIDimension min_secondary = item.widget->effective_min_size().secondary_size_for_orientation(orientation());
            UIDimension max_secondary = item.widget->max_size().secondary_size_for_orientation(orientation());
            UIDimension preferred_secondary = item.widget->effective_preferred_size().secondary_size_for_orientation(orientation());
            if (preferred_secondary.is_int())
                secondary = min(secondary, preferred_secondary.as_int());
            if (min_secondary.is_int())
                secondary = max(secondary, min_secondary.as_int());
            if (max_secondary.is_int())
                secondary = min(secondary, max_secondary.as_int());

            rect.set_secondary_size_for_orientation(orientation(), secondary);

            if (orientation() == Gfx::Orientation::Horizontal)
                rect.center_vertically_within(widget_rect_with_margins_subtracted);
            else
                rect.center_horizontally_within(widget_rect_with_margins_subtracted);

            item.widget->set_relative_rect(rect);

            if (orientation() == Gfx::Orientation::Horizontal)
                current_x += rect.width() + spacing();
            else
                current_y += rect.height() + spacing();
        } else {
            if (orientation() == Gfx::Orientation::Horizontal)
                current_x += spacer_width;
            else
                current_y += spacer_width;
        }
    }
}

}
