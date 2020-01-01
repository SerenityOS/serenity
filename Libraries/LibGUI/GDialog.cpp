#include <LibGUI/GDesktop.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GEvent.h>

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
    m_event_loop = make<CEventLoop>();
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
    remove_from_parent();
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

void GDialog::event(CEvent& event)
{
    if (event.type() == GEvent::KeyUp) {
        auto& key_event = static_cast<GKeyEvent&>(event);
        if (key_event.key() == KeyCode::Key_Escape) {
            done(ExecCancel);
            return;
        }
    }

    GWindow::event(event);
}

void GDialog::close()
{
    GWindow::close();
    m_event_loop->quit(ExecCancel);
}
