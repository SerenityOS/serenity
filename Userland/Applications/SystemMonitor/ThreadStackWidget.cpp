/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThreadStackWidget.h"
#include <AK/ByteBuffer.h>
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibSymbolication/Symbolication.h>
#include <LibThreading/BackgroundAction.h>

ThreadStackWidget::ThreadStackWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    m_stack_editor = add<GUI::TextEditor>();
    m_stack_editor->set_mode(GUI::TextEditor::ReadOnly);
    m_stack_editor->set_text("Symbolicating...");
}

ThreadStackWidget::~ThreadStackWidget()
{
}

void ThreadStackWidget::show_event(GUI::ShowEvent&)
{
    refresh();
    if (!m_timer)
        m_timer = add<Core::Timer>(1000, [this] { refresh(); });
}

void ThreadStackWidget::hide_event(GUI::HideEvent&)
{
    m_timer = nullptr;
}

void ThreadStackWidget::set_ids(pid_t pid, pid_t tid)
{
    if (m_pid == pid && m_tid == tid)
        return;
    m_pid = pid;
    m_tid = tid;
}

class CompletionEvent : public Core::CustomEvent {
public:
    explicit CompletionEvent(Vector<Symbolication::Symbol> symbols)
        : Core::CustomEvent(0)
        , m_symbols(move(symbols))
    {
    }

    Vector<Symbolication::Symbol> const& symbols() const { return m_symbols; }

private:
    Vector<Symbolication::Symbol> m_symbols;
};

void ThreadStackWidget::refresh()
{
    Threading::BackgroundAction<Vector<Symbolication::Symbol>>::create(
        [pid = m_pid, tid = m_tid] {
            return Symbolication::symbolicate_thread(pid, tid);
        },

        [weak_this = make_weak_ptr()](auto result) {
            if (!weak_this)
                return;
            Core::EventLoop::main().post_event(const_cast<Core::Object&>(*weak_this), make<CompletionEvent>(move(result)));
        });
}

void ThreadStackWidget::custom_event(Core::CustomEvent& event)
{
    auto& completion_event = downcast<CompletionEvent>(event);

    StringBuilder builder;

    for (auto& symbol : completion_event.symbols()) {
        builder.appendff("{:p}", symbol.address);
        if (!symbol.name.is_empty())
            builder.appendff("  {}", symbol.name);
        builder.append('\n');
    }

    if (m_stack_editor->text() != builder.string_view()) {
        m_stack_editor->set_text(builder.string_view());
    }
}
