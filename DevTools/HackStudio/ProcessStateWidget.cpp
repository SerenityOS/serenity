#include "ProcessStateWidget.h"
#include <LibCore/CProcessStatisticsReader.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <unistd.h>

ProcessStateWidget::ProcessStateWidget(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 20);
    set_visible(false);

    set_layout(make<GBoxLayout>(Orientation::Horizontal));

    auto pid_label_label = GLabel::construct("Process:", this);
    pid_label_label->set_font(Font::default_bold_font());
    m_pid_label = GLabel::construct("", this);

    auto state_label_label = GLabel::construct("State:", this);
    state_label_label->set_font(Font::default_bold_font());
    m_state_label = GLabel::construct("", this);

    // FIXME: This should show CPU% instead.
    auto cpu_label_label = GLabel::construct("Times scheduled:", this);
    cpu_label_label->set_font(Font::default_bold_font());
    m_cpu_label = GLabel::construct("", this);

    auto memory_label_label = GLabel::construct("Memory (resident):", this);
    memory_label_label->set_font(Font::default_bold_font());
    m_memory_label = GLabel::construct("", this);

    m_timer = CTimer::construct(500, [this] {
        refresh();
    });
}

ProcessStateWidget::~ProcessStateWidget()
{
}

void ProcessStateWidget::refresh()
{
    pid_t pid = tcgetpgrp(m_tty_fd);

    auto processes = CProcessStatisticsReader::get_all();
    auto child_process_data = processes.get(pid);

    if (!child_process_data.has_value())
        return;

    auto active_process_data = processes.get(child_process_data.value().pgid);

    auto& data = active_process_data.value();

    m_pid_label->set_text(String::format("%s(%d)", data.name.characters(), pid));
    m_state_label->set_text(data.threads.first().state);
    m_cpu_label->set_text(String::format("%d", data.threads.first().times_scheduled));
    m_memory_label->set_text(String::format("%d", data.amount_resident));
}

void ProcessStateWidget::set_tty_fd(int tty_fd)
{
    m_tty_fd = tty_fd;
    if (m_tty_fd == -1) {
        set_visible(false);
        return;
    }
    set_visible(true);
    refresh();
}
