#include <LibGUI/GDesktop.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GEventLoop.h>

GDialog::GDialog(CObject* parent)
    : GWindow(parent)
{
    set_modal(true);

}

GDialog::~GDialog()
{
}

int GDialog::exec()
{
    ASSERT(!m_event_loop);
    m_event_loop = make<GEventLoop>();
    auto new_rect = rect();
    if (parent() && parent()->is_window()) {
        auto& parent_window = *static_cast<GWindow*>(parent());
        new_rect.center_within(parent_window.rect());
    } else {
        new_rect.center_within(GDesktop::the().rect());
    }
    set_rect(new_rect);
    show();
    auto result = m_event_loop->exec();
    m_event_loop = nullptr;
    dbgprintf("%s: event loop returned with result %d\n", class_name(), result);
    return result;
}

void GDialog::done(int result)
{
    if (!m_event_loop)
        return;
    m_result = result;
    dbgprintf("%s: quit event loop with result %d\n", class_name(), result);
    m_event_loop->quit(result);
}

void GDialog::close()
{
    GWindow::close();
    m_event_loop->quit(ExecCancel);
}

