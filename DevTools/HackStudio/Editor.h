#pragma once

#include <LibGUI/GTextEditor.h>

class EditorWrapper;
class HtmlView;

class Editor final : public GTextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override;

    Function<void()> on_focus;

    EditorWrapper& wrapper();
    const EditorWrapper& wrapper() const;

private:
    virtual void focusin_event(CEvent&) override;
    virtual void focusout_event(CEvent&) override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void cursor_did_change() override;

    void show_documentation_tooltip_if_available(const String&, const Point& screen_location);

    explicit Editor(GWidget* parent);

    RefPtr<GWindow> m_documentation_tooltip_window;
    RefPtr<HtmlView> m_documentation_html_view;
    String m_last_parsed_token;

    struct BuddySpan {
        int index { -1 };
        GTextDocumentSpan span_backup;
    };

    bool m_has_brace_buddies { false };
    BuddySpan m_brace_buddies[2];
};
