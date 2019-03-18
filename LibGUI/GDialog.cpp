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
    GEventLoop loop;
    show();
    return loop.exec();
}

void GDialog::done(int result)
{
    m_result = result;
    GEventLoop::current().quit(result);
}
