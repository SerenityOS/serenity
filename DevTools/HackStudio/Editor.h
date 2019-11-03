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

    void show_documentation_tooltip_if_available(const String&, const Point& screen_location);

    explicit Editor(GWidget* parent);

    RefPtr<GWindow> m_documentation_tooltip_window;
    RefPtr<HtmlView> m_documentation_html_view;
    String m_last_parsed_token;
};
