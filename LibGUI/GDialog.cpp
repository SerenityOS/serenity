#include <LibGUI/GDialog.h>
#include <LibGUI/GEventLoop.h>

GDialog::GDialog(GObject* parent)
    : GWindow(parent)
{
    set_modal(true);
    set_should_exit_event_loop_on_close(true);
}

GDialog::~GDialog()
{
}

int GDialog::exec()
{
    ASSERT(!m_event_loop);
    m_event_loop = make<GEventLoop>();
    if (parent() && parent()->is_window()) {
        auto& parent_window = *static_cast<GWindow*>(parent());
        auto new_rect = rect();
        new_rect.center_within(parent_window.rect());
        set_rect(new_rect);
    }
    show();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgprintf("event loop returned with result %d\n", result);
    return result;
}

void GDialog::done(int result)
{
    ASSERT(m_event_loop);
    m_result = result;
    dbgprintf("quit event loop with result %d\n", result);
    m_event_loop->quit(result);
}
