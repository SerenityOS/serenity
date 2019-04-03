#include "MemoryStatsWidget.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <SharedGraphics/StylePainter.h>
#include <stdio.h>
#include <stdlib.h>

MemoryStatsWidget::MemoryStatsWidget(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 60 });

    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 0, 8, 0, 0 });
    layout()->set_spacing(3);

    auto build_widgets_for_label = [this] (const String& description) -> GLabel* {
        auto* container = new GWidget(this);
        container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        container->set_preferred_size({ 250, 12 });
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

    start_timer(1000);
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
    FILE* fp = fopen("/proc/memstat", "r");
    if (!fp) {
        perror("failed to open /proc/memstat");
        exit(1);
    }

    for (;;) {
        char buf[BUFSIZ];
        char* ptr = fgets(buf, sizeof(buf), fp);
        if (!ptr)
            break;
        auto parts = String(buf, Chomp).split(',');
        if (parts.size() < 7)
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

        size_t kmalloc_sum_available = kmalloc_sum_alloc + kmalloc_sum_free;
        size_t user_pages_available = user_pages_alloc + user_pages_free;
        size_t supervisor_pages_available = supervisor_pages_alloc + supervisor_pages_free;

        m_kmalloc_label->set_text(String::format("%uK/%uK\n", bytes_to_kb(kmalloc_sum_alloc), bytes_to_kb(kmalloc_sum_available)));
        m_user_physical_pages_label->set_text(String::format("%uK/%uK\n", page_count_to_kb(user_pages_alloc), page_count_to_kb(user_pages_available)));
        m_supervisor_physical_pages_label->set_text(String::format("%uK/%uK\n", page_count_to_kb(supervisor_pages_alloc), page_count_to_kb(supervisor_pages_available)));
        break;
    }

    fclose(fp);
}

void MemoryStatsWidget::timer_event(GTimerEvent&)
{
    refresh();
}

void MemoryStatsWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    StylePainter::paint_surface(painter, rect());
}
