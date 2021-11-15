/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MemoryStatsWidget.h"
#include "GraphWidget.h"
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/StylePainter.h>
#include <stdlib.h>

static MemoryStatsWidget* s_the;

MemoryStatsWidget* MemoryStatsWidget::the()
{
    return s_the;
}

MemoryStatsWidget::MemoryStatsWidget(GraphWidget& graph)
    : m_graph(graph)
{
    VERIFY(!s_the);
    s_the = this;

    set_fixed_height(110);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this](const String& description) -> RefPtr<GUI::Label> {
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

    m_user_physical_pages_label = build_widgets_for_label("Physical memory:");
    m_user_physical_pages_committed_label = build_widgets_for_label("Committed memory:");
    m_supervisor_physical_pages_label = build_widgets_for_label("Supervisor physical:");
    m_kmalloc_space_label = build_widgets_for_label("Kernel heap:");
    m_kmalloc_count_label = build_widgets_for_label("Calls kmalloc:");
    m_kfree_count_label = build_widgets_for_label("Calls kfree:");
    m_kmalloc_difference_label = build_widgets_for_label("Difference:");

    refresh();
}

MemoryStatsWidget::~MemoryStatsWidget()
{
}

static inline u64 page_count_to_bytes(size_t count)
{
    return count * 4096;
}

static inline u64 page_count_to_kb(u64 count)
{
    return page_count_to_bytes(count) / 1024;
}

static inline u64 bytes_to_kb(u64 bytes)
{
    return bytes / 1024;
}

void MemoryStatsWidget::refresh()
{
    auto proc_memstat = Core::File::construct("/proc/memstat");
    if (!proc_memstat->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto file_contents = proc_memstat->read_all();
    auto json_result = JsonValue::from_string(file_contents).release_value_but_fixme_should_propagate_errors();
    auto const& json = json_result.as_object();

    [[maybe_unused]] u32 kmalloc_eternal_allocated = json.get("kmalloc_eternal_allocated").to_u32();
    u32 kmalloc_allocated = json.get("kmalloc_allocated").to_u32();
    u32 kmalloc_available = json.get("kmalloc_available").to_u32();
    u64 user_physical_allocated = json.get("user_physical_allocated").to_u64();
    u64 user_physical_available = json.get("user_physical_available").to_u64();
    u64 user_physical_committed = json.get("user_physical_committed").to_u64();
    u64 user_physical_uncommitted = json.get("user_physical_uncommitted").to_u64();
    u64 super_physical_alloc = json.get("super_physical_allocated").to_u64();
    u64 super_physical_free = json.get("super_physical_available").to_u64();
    u32 kmalloc_call_count = json.get("kmalloc_call_count").to_u32();
    u32 kfree_call_count = json.get("kfree_call_count").to_u32();

    u64 kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
    u64 user_physical_pages_total = user_physical_allocated + user_physical_available;
    u64 supervisor_pages_total = super_physical_alloc + super_physical_free;

    u64 physical_pages_total = user_physical_pages_total + supervisor_pages_total;
    u64 physical_pages_in_use = user_physical_allocated + super_physical_alloc;
    u64 total_userphysical_and_swappable_pages = user_physical_allocated + user_physical_committed + user_physical_uncommitted;

    m_kmalloc_space_label->set_text(String::formatted("{}/{}", human_readable_size(kmalloc_allocated), human_readable_size(kmalloc_bytes_total)));
    m_user_physical_pages_label->set_text(String::formatted("{}/{}", human_readable_size(page_count_to_bytes(physical_pages_in_use)), human_readable_size(page_count_to_bytes(physical_pages_total))));
    m_user_physical_pages_committed_label->set_text(String::formatted("{}", human_readable_size(page_count_to_bytes(user_physical_committed))));
    m_supervisor_physical_pages_label->set_text(String::formatted("{}/{}", human_readable_size(page_count_to_bytes(super_physical_alloc)), human_readable_size(page_count_to_bytes(supervisor_pages_total))));
    m_kmalloc_count_label->set_text(String::formatted("{}", kmalloc_call_count));
    m_kfree_count_label->set_text(String::formatted("{}", kfree_call_count));
    m_kmalloc_difference_label->set_text(String::formatted("{:+}", kmalloc_call_count - kfree_call_count));

    m_graph.set_max(page_count_to_bytes(total_userphysical_and_swappable_pages) + kmalloc_bytes_total);
    m_graph.add_value({ page_count_to_bytes(user_physical_committed), page_count_to_bytes(user_physical_allocated), kmalloc_bytes_total });
}
