#include "MemoryStatsWidget.h"
#include "GraphWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/StylePainter.h>
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
    set_preferred_size({ 0, 72 });

    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 0, 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this](const String& description) -> GLabel* {
        auto* container = new GWidget(this);
        container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        container->set_preferred_size({ 255, 12 });
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

    for (;;) {
        auto line = m_proc_memstat.read_line(BUFSIZ);
        if (line.is_null())
            break;
        auto chomped = String((const char*)line.pointer(), line.size() - 1, Chomp);
        auto parts = chomped.split_view(',');
        if (parts.size() < 9)
            break;
        bool ok;
        unsigned kmalloc_sum_eternal = parts[0].to_uint(ok);
        ASSERT(ok);
        (void)kmalloc_sum_eternal;
        unsigned kmalloc_sum_alloc = parts[1].to_uint(ok);
        ASSERT(ok);
        unsigned kmalloc_sum_free = parts[2].to_uint(ok);
        ASSERT(ok);
        unsigned user_pages_alloc = parts[3].to_uint(ok);
        ASSERT(ok);
        unsigned user_pages_free = parts[4].to_uint(ok);
        ASSERT(ok);
        unsigned supervisor_pages_alloc = parts[5].to_uint(ok);
        ASSERT(ok);
        unsigned supervisor_pages_free = parts[6].to_uint(ok);
        ASSERT(ok);
        unsigned kmalloc_call_count = parts[7].to_uint(ok);
        ASSERT(ok);
        unsigned kfree_call_count = parts[8].to_uint(ok);
        ASSERT(ok);

        size_t kmalloc_sum_available = kmalloc_sum_alloc + kmalloc_sum_free;
        size_t user_pages_available = user_pages_alloc + user_pages_free;
        size_t supervisor_pages_available = supervisor_pages_alloc + supervisor_pages_free;

        m_kmalloc_label->set_text(String::format("%uK/%uK", bytes_to_kb(kmalloc_sum_alloc), bytes_to_kb(kmalloc_sum_available)));
        m_user_physical_pages_label->set_text(String::format("%uK/%uK", page_count_to_kb(user_pages_alloc), page_count_to_kb(user_pages_available)));
        m_supervisor_physical_pages_label->set_text(String::format("%uK/%uK", page_count_to_kb(supervisor_pages_alloc), page_count_to_kb(supervisor_pages_available)));
        m_kmalloc_count_label->set_text(String::format("%u/%u (+%u)", kmalloc_call_count, kfree_call_count, kmalloc_call_count - kfree_call_count));

        m_graph.set_max(page_count_to_kb(user_pages_available));
        m_graph.add_value(page_count_to_kb(user_pages_alloc));
    }
}

void MemoryStatsWidget::timer_event(CTimerEvent&)
{
    refresh();
}
