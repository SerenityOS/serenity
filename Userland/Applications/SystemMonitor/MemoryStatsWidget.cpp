/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MemoryStatsWidget.h"
#include "GraphWidget.h"
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/File.h>
#include <LibCore/Object.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/StylePainter.h>
#include <stdlib.h>

REGISTER_WIDGET(SystemMonitor, MemoryStatsWidget)

namespace SystemMonitor {

static MemoryStatsWidget* s_the;

MemoryStatsWidget* MemoryStatsWidget::the()
{
    return s_the;
}

MemoryStatsWidget::MemoryStatsWidget()
    : MemoryStatsWidget(nullptr)
{
}

MemoryStatsWidget::MemoryStatsWidget(GraphWidget* graph)
    : m_graph(graph)
{
    VERIFY(!s_the);
    s_the = this;

    REGISTER_STRING_PROPERTY("memory_graph", graph_widget_name, set_graph_widget_via_name);

    set_fixed_height(110);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this](String const& description) -> RefPtr<GUI::Label> {
        auto& container = add<GUI::Widget>();
        container.set_layout<GUI::HorizontalBoxLayout>();
        container.set_fixed_size(275, 12);
        auto& description_label = container.add<GUI::Label>(description);
        description_label.set_font(Gfx::FontDatabase::default_font().bold_variant());
        description_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        auto& label = container.add<GUI::Label>();
        label.set_text_alignment(Gfx::TextAlignment::CenterRight);
        return label;
    };

    m_physical_pages_label = build_widgets_for_label("Physical memory:");
    m_physical_pages_committed_label = build_widgets_for_label("Committed memory:");
    m_kmalloc_space_label = build_widgets_for_label("Kernel heap:");
    m_kmalloc_count_label = build_widgets_for_label("Calls kmalloc:");
    m_kfree_count_label = build_widgets_for_label("Calls kfree:");
    m_kmalloc_difference_label = build_widgets_for_label("Difference:");

    refresh();
}

void MemoryStatsWidget::set_graph_widget(GraphWidget& graph)
{
    m_graph = &graph;
}

void MemoryStatsWidget::set_graph_widget_via_name(String name)
{
    m_graph_widget_name = move(name);
    if (!m_graph_widget_name.is_null()) {
        // FIXME: We assume here that the graph widget is a sibling or descendant of a sibling. This prevents more complex hierarchies.
        auto* maybe_graph = parent_widget()->find_descendant_of_type_named<GraphWidget>(m_graph_widget_name);
        if (maybe_graph) {
            m_graph = maybe_graph;
            // Delete the stored graph name to signal that we found the widget
            m_graph_widget_name = {};
        } else {
            dbgln("MemoryStatsWidget: Couldn't find graph of name '{}', retrying later.", m_graph_widget_name);
        }
    }
}

String MemoryStatsWidget::graph_widget_name()
{
    if (m_graph)
        return m_graph->name();
    return m_graph_widget_name;
}

static inline u64 page_count_to_bytes(size_t count)
{
    return count * 4096;
}

void MemoryStatsWidget::refresh()
{
    auto proc_memstat = Core::File::construct("/sys/kernel/memstat");
    if (!proc_memstat->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto file_contents = proc_memstat->read_all();
    auto json_result = JsonValue::from_string(file_contents).release_value_but_fixme_should_propagate_errors();
    auto const& json = json_result.as_object();

    u32 kmalloc_allocated = json.get("kmalloc_allocated"sv).to_u32();
    u32 kmalloc_available = json.get("kmalloc_available"sv).to_u32();
    u64 physical_allocated = json.get("physical_allocated"sv).to_u64();
    u64 physical_available = json.get("physical_available"sv).to_u64();
    u64 physical_committed = json.get("physical_committed"sv).to_u64();
    u64 physical_uncommitted = json.get("physical_uncommitted"sv).to_u64();
    u32 kmalloc_call_count = json.get("kmalloc_call_count"sv).to_u32();
    u32 kfree_call_count = json.get("kfree_call_count"sv).to_u32();

    u64 kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
    u64 physical_pages_total = physical_allocated + physical_available;

    u64 physical_pages_in_use = physical_allocated;
    u64 total_userphysical_and_swappable_pages = physical_allocated + physical_committed + physical_uncommitted;

    m_kmalloc_space_label->set_text(String::formatted("{}/{}", human_readable_size(kmalloc_allocated), human_readable_size(kmalloc_bytes_total)));
    m_physical_pages_label->set_text(String::formatted("{}/{}", human_readable_size(page_count_to_bytes(physical_pages_in_use)), human_readable_size(page_count_to_bytes(physical_pages_total))));
    m_physical_pages_committed_label->set_text(String::formatted("{}", human_readable_size(page_count_to_bytes(physical_committed))));
    m_kmalloc_count_label->set_text(String::formatted("{}", kmalloc_call_count));
    m_kfree_count_label->set_text(String::formatted("{}", kfree_call_count));
    m_kmalloc_difference_label->set_text(String::formatted("{:+}", kmalloc_call_count - kfree_call_count));

    // Because the initialization order of us and the graph is unknown, we might get a couple of updates where the graph widget lookup fails.
    // Therefore, we can retry indefinitely. (Should not be too much of a performance hit, as we don't update that often.)
    if (!m_graph)
        set_graph_widget_via_name(move(m_graph_widget_name));

    if (m_graph) {
        m_graph->set_max(page_count_to_bytes(total_userphysical_and_swappable_pages) + kmalloc_bytes_total);
        m_graph->add_value({ page_count_to_bytes(physical_committed), page_count_to_bytes(physical_allocated), kmalloc_bytes_total });
    }
}

}
