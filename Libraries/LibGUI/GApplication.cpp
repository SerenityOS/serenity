#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>
#include <WindowServer/WSAPITypes.h>

static GApplication* s_the;

GApplication& GApplication::the()
{
    ASSERT(s_the);
    return *s_the;
}

GApplication::GApplication(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    ASSERT(!s_the);
    s_the = this;
    m_event_loop = make<CEventLoop>();
    GWindowServerConnection::the();
    if (argc > 0)
        m_invoked_as = argv[0];
    for (int i = 1; i < argc; i++)
        m_args.append(argv[i]);
}

GApplication::~GApplication()
{
    s_the = nullptr;
}

int GApplication::exec()
{
    int exit_code = m_event_loop->exec();
    // NOTE: Maybe it would be cool to return instead of exit()?
    //       This would require cleaning up all the CObjects on the heap.
    exit(exit_code);
    return exit_code;
}

void GApplication::quit(int exit_code)
{
    m_event_loop->quit(exit_code);
}

void GApplication::set_menubar(OwnPtr<GMenuBar>&& menubar)
{
    if (m_menubar)
        m_menubar->notify_removed_from_application({});
    m_menubar = move(menubar);
    if (m_menubar)
        m_menubar->notify_added_to_application({});
}

void GApplication::register_global_shortcut_action(Badge<GAction>, GAction& action)
{
    m_global_shortcut_actions.set(action.shortcut(), &action);
}

void GApplication::unregister_global_shortcut_action(Badge<GAction>, GAction& action)
{
    m_global_shortcut_actions.remove(action.shortcut());
}

GAction* GApplication::action_for_key_event(const GKeyEvent& event)
{
    auto it = m_global_shortcut_actions.find(GShortcut(event.modifiers(), (KeyCode)event.key()));
    if (it == m_global_shortcut_actions.end())
        return nullptr;
    return (*it).value;
}

class GApplication::TooltipWindow final : public GWindow {
public:
    TooltipWindow()
    {
        set_window_type(GWindowType::Tooltip);
        m_label = GLabel::construct();
        m_label->set_background_color(Color::from_rgb(0xdac7b5));
        m_label->set_fill_with_background_color(true);
        m_label->set_frame_thickness(1);
        m_label->set_frame_shape(FrameShape::Container);
        m_label->set_frame_shadow(FrameShadow::Plain);
        set_main_widget(m_label);
    }

    void set_tooltip(const StringView& tooltip)
    {
        // FIXME: Add some kind of GLabel auto-sizing feature.
        int text_width = m_label->font().width(tooltip);
        set_rect(100, 100, text_width + 10, m_label->font().glyph_height() + 8);
        m_label->set_text(tooltip);
    }

    RefPtr<GLabel> m_label;
};

void GApplication::show_tooltip(const StringView& tooltip, const Point& screen_location)
{
    if (!m_tooltip_window) {
        m_tooltip_window = new TooltipWindow;
        m_tooltip_window->set_double_buffering_enabled(false);
    }
    m_tooltip_window->set_tooltip(tooltip);
    m_tooltip_window->move_to(screen_location);
    m_tooltip_window->show();
}

void GApplication::hide_tooltip()
{
    if (m_tooltip_window) {
        m_tooltip_window->hide();
        m_tooltip_window = nullptr;
    }
}

void GApplication::did_create_window(Badge<GWindow>)
{
    if (m_event_loop->was_exit_requested())
        m_event_loop->unquit();
}

void GApplication::did_delete_last_window(Badge<GWindow>)
{
    if (m_quit_when_last_window_deleted)
        m_event_loop->quit(0);
}
