#include "MemoryStatsWidget.h"
#include "GraphWidget.h"
#include <AK/JsonObject.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibDraw/StylePainter.h>
#include <stdio.h>
#include <stdlib.h>

MemoryStatsWidget::MemoryStatsWidget(GraphWidget& graph, GWidget* parent)
    : GWidget(parent)
    , m_graph(graph)
    , m_proc_memstat("/proc/memstat")
{
    if (!m_proc_memstat.open(CIODevice::OpenMode::ReadOnly))
        ASSERT_NOT_REACHED();
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 72);

    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 0, 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this](const String& description) -> GLabel* {
        auto* container = new GWidget(this);
        container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        container->set_preferred_size(255, 12);
        auto* description_label = new GLabel(description, container);
        description_label->set_font(Font::default_bold_font());
        description_label->set_text_alignment(TextAlignment::CenterLeft);
        auto* label = new GLabel(container);
        label->set_text_alignment(TextAlignment::CenterRight);
        return label;
    };

    m_user_physical_pages_label = build_widgets_for_label("Userspace physical:");
    m_supervisor_physical_pages_label = build_widgets_for_label("Supervisor physical:");
    m_kmalloc_label = build_widgets_for_label("Kernel heap:");
    m_kmalloc_count_label = build_widgets_for_label("Calls kmalloc/kfree:");

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
    m_proc_memstat.seek(0);

    auto file_contents = m_proc_memstat.read_all();
    auto json = JsonValue::from_string(file_contents).as_object();

    unsigned kmalloc_eternal_allocated = json.get("kmalloc_eternal_allocated").to_u32();
    (void)kmalloc_eternal_allocated;
    unsigned kmalloc_allocated = json.get("kmalloc_allocated").to_u32();
    unsigned kmalloc_available = json.get("kmalloc_available").to_u32();
    unsigned user_physical_allocated = json.get("user_physical_allocated").to_u32();
    unsigned user_physical_available = json.get("user_physical_available").to_u32();
    unsigned super_physical_alloc = json.get("super_physical_allocated").to_u32();
    unsigned super_physical_free = json.get("super_physical_available").to_u32();
    unsigned kmalloc_call_count = json.get("kmalloc_call_count").to_u32();
    unsigned kfree_call_count = json.get("kfree_call_count").to_u32();

    size_t kmalloc_sum_available = kmalloc_allocated + kmalloc_available;
    size_t user_pages_available = user_physical_allocated + user_physical_available;
    size_t supervisor_pages_available = super_physical_alloc + super_physical_free;

    m_kmalloc_label->set_text(String::format("%uK/%uK", bytes_to_kb(kmalloc_allocated), bytes_to_kb(kmalloc_sum_available)));
    m_user_physical_pages_label->set_text(String::format("%uK/%uK", page_count_to_kb(user_physical_allocated), page_count_to_kb(user_pages_available)));
    m_supervisor_physical_pages_label->set_text(String::format("%uK/%uK", page_count_to_kb(super_physical_alloc), page_count_to_kb(supervisor_pages_available)));
    m_kmalloc_count_label->set_text(String::format("%u/%u (+%u)", kmalloc_call_count, kfree_call_count, kmalloc_call_count - kfree_call_count));

    m_graph.set_max(page_count_to_kb(user_pages_available));
    m_graph.add_value(page_count_to_kb(user_physical_allocated));
}

void MemoryStatsWidget::timer_event(CTimerEvent&)
{
    refresh();
}
