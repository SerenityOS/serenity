/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThreadStackWidget.h"
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>
#include <LibSymbolication/Symbolication.h>
#include <LibThreading/BackgroundAction.h>

REGISTER_WIDGET(SystemMonitor, ThreadStackWidget)

namespace SystemMonitor {

class ThreadStackModel final : public GUI::Model {

    enum Column {
        Address,
        Object,
        Symbol
    };

public:
    int column_count(GUI::ModelIndex const&) const override { return 3; }
    int row_count(GUI::ModelIndex const&) const override { return m_symbols.size(); }
    bool is_column_sortable(int) const override { return false; }

    ErrorOr<String> column_name(int column) const override
    {
        switch (column) {
        case Column::Address:
            return "Address"_string;
        case Column::Object:
            return "Object"_string;
        case Column::Symbol:
            return "Symbol"_string;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    GUI::Variant data(GUI::ModelIndex const& model_index, GUI::ModelRole) const override
    {
        auto& symbol = m_symbols[model_index.row()];
        switch (model_index.column()) {
        case Column::Address:
            return ByteString::formatted("{:p}", symbol.address);
        case Column::Object:
            return symbol.object;
        case Column::Symbol:
            return symbol.name;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    void set_symbols(Vector<Symbolication::Symbol> const& symbols)
    {
        if (m_symbols == symbols)
            return;
        m_symbols = symbols;
        invalidate();
    }

private:
    Vector<Symbolication::Symbol> m_symbols;
};

ErrorOr<NonnullRefPtr<ThreadStackWidget>> ThreadStackWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ThreadStackWidget()));
    widget->set_layout<GUI::VerticalBoxLayout>(4);
    widget->m_stack_table = widget->add<GUI::TableView>();
    widget->m_stack_table->set_model(TRY(try_make_ref_counted<ThreadStackModel>()));
    return widget;
}

void ThreadStackWidget::show_event(GUI::ShowEvent&)
{
    refresh();
    if (!m_timer) {
        m_timer = add<Core::Timer>(1000, [this] { refresh(); });
        m_timer->start();
    }
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
    (void)Threading::BackgroundAction<Vector<Symbolication::Symbol>>::construct(
        [pid = m_pid, tid = m_tid](auto&) {
            return Symbolication::symbolicate_thread(pid, tid, Symbolication::IncludeSourcePosition::No);
        },

        [weak_this = make_weak_ptr()](auto result) -> ErrorOr<void> {
            if (!weak_this)
                return {};
            Core::EventLoop::current().post_event(const_cast<Core::EventReceiver&>(*weak_this), make<CompletionEvent>(move(result)));
            return {};
        });
}

void ThreadStackWidget::custom_event(Core::CustomEvent& event)
{
    auto& completion_event = verify_cast<CompletionEvent>(event);
    verify_cast<ThreadStackModel>(m_stack_table->model())->set_symbols(completion_event.symbols());
}

}
