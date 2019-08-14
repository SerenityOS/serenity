#include "ProcessStacksWidget.h"
#include <LibCore/CFile.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GBoxLayout.h>

ProcessStacksWidget::ProcessStacksWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_stacks_editor = new GTextEditor(GTextEditor::Type::MultiLine, this);
    m_stacks_editor->set_readonly(true);

    m_timer = new CTimer(1000, [this] { refresh(); }, this);
}

ProcessStacksWidget::~ProcessStacksWidget()
{
}

void ProcessStacksWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    refresh();
}

void ProcessStacksWidget::refresh()
{
    CFile file(String::format("/proc/%d/stack", m_pid));
    if (!file.open(CIODevice::ReadOnly)) {
        m_stacks_editor->set_text(String::format("Unable to open %s", file.filename().characters()));
        return;
    }

    m_stacks_editor->set_text(file.read_all());
}
