#include "MemoryStatsWidget.h"
#include <SharedGraphics/Painter.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GStyle.h>
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

    m_user_physical_pages_label = new GLabel(this);
    m_user_physical_pages_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_user_physical_pages_label->set_preferred_size({ 0, 12 });
    m_user_physical_pages_label->set_fill_with_background_color(false);
    m_supervisor_physical_pages_label = new GLabel(this);
    m_supervisor_physical_pages_label->set_fill_with_background_color(false);
    m_supervisor_physical_pages_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_supervisor_physical_pages_label->set_preferred_size({ 0, 12 });
    m_kmalloc_label = new GLabel(this);
    m_kmalloc_label->set_fill_with_background_color(false);
    m_kmalloc_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_kmalloc_label->set_preferred_size({ 0, 12 });

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

        m_kmalloc_label->set_text(String::format("Kernel heap: %uK allocated, %uK free\n", bytes_to_kb(kmalloc_sum_alloc), bytes_to_kb(kmalloc_sum_free)));
        m_user_physical_pages_label->set_text(String::format("Userspace physical: %uK allocated, %uK free\n", page_count_to_kb(user_pages_alloc), page_count_to_kb(user_pages_free)));
        m_supervisor_physical_pages_label->set_text(String::format("Supervisor physical: %uK allocated, %uK free\n", page_count_to_kb(supervisor_pages_alloc), page_count_to_kb(supervisor_pages_free)));
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
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    GStyle::the().paint_surface(painter, rect());
}
