/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "MemoryStatsWidget.h"
#include "GraphWidget.h"
#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/StylePainter.h>
#include <stdio.h>
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
    layout()->set_margins({ 0, 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this](const String& description) -> RefPtr<GUI::Label> {
        auto& container = add<GUI::Widget>();
        container.set_layout<GUI::HorizontalBoxLayout>();
        container.set_fixed_size(275, 12);
        auto& description_label = container.add<GUI::Label>(description);
        description_label.set_font(Gfx::FontDatabase::default_bold_font());
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

static inline size_t page_count_to_kb(size_t kb)
{
    return (kb * 4096) / 1024;
}

static inline size_t bytes_to_kb(size_t bytes)
{
    return bytes / 1024;
}

void MemoryStatsWidget::refresh()
{
    auto proc_memstat = Core::File::construct("/proc/memstat");
    if (!proc_memstat->open(Core::IODevice::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto file_contents = proc_memstat->read_all();
    auto json_result = JsonValue::from_string(file_contents);
    VERIFY(json_result.has_value());
    auto json = json_result.value().as_object();

    [[maybe_unused]] unsigned kmalloc_eternal_allocated = json.get("kmalloc_eternal_allocated").to_u32();
    unsigned kmalloc_allocated = json.get("kmalloc_allocated").to_u32();
    unsigned kmalloc_available = json.get("kmalloc_available").to_u32();
    unsigned user_physical_allocated = json.get("user_physical_allocated").to_u32();
    unsigned user_physical_available = json.get("user_physical_available").to_u32();
    unsigned user_physical_committed = json.get("user_physical_committed").to_u32();
    unsigned user_physical_uncommitted = json.get("user_physical_uncommitted").to_u32();
    unsigned super_physical_alloc = json.get("super_physical_allocated").to_u32();
    unsigned super_physical_free = json.get("super_physical_available").to_u32();
    unsigned kmalloc_call_count = json.get("kmalloc_call_count").to_u32();
    unsigned kfree_call_count = json.get("kfree_call_count").to_u32();

    size_t kmalloc_bytes_total = kmalloc_allocated + kmalloc_available;
    size_t user_physical_pages_total = user_physical_allocated + user_physical_available;
    size_t supervisor_pages_total = super_physical_alloc + super_physical_free;

    size_t physical_pages_total = user_physical_pages_total + supervisor_pages_total;
    size_t physical_pages_in_use = user_physical_allocated + super_physical_alloc;
    size_t total_userphysical_and_swappable_pages = user_physical_allocated + user_physical_committed + user_physical_uncommitted;

    m_kmalloc_space_label->set_text(String::formatted("{}K/{}K", bytes_to_kb(kmalloc_allocated), bytes_to_kb(kmalloc_bytes_total)));
    m_user_physical_pages_label->set_text(String::formatted("{}K/{}K", page_count_to_kb(physical_pages_in_use), page_count_to_kb(physical_pages_total)));
    m_user_physical_pages_committed_label->set_text(String::formatted("{}K", page_count_to_kb(user_physical_committed)));
    m_supervisor_physical_pages_label->set_text(String::formatted("{}K/{}K", page_count_to_kb(super_physical_alloc), page_count_to_kb(supervisor_pages_total)));
    m_kmalloc_count_label->set_text(String::formatted("{}", kmalloc_call_count));
    m_kfree_count_label->set_text(String::formatted("{}", kfree_call_count));
    m_kmalloc_difference_label->set_text(String::formatted("{:+}", kmalloc_call_count - kfree_call_count));

    m_graph.set_max(page_count_to_kb(total_userphysical_and_swappable_pages) + bytes_to_kb(kmalloc_bytes_total));
    m_graph.add_value({ (int)page_count_to_kb(user_physical_committed), (int)page_count_to_kb(user_physical_allocated), (int)bytes_to_kb(kmalloc_bytes_total) });
}
