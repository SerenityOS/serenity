#pragma once

#include <LibGUI/GWidget.h>

class LocatorTextBox;
class GTableView;

class Locator final : public GWidget {
    C_OBJECT(Locator)
public:
    virtual ~Locator() override;

    void open();
    void close();

private:
    virtual void keydown_event(GKeyEvent&) override;

    void update_suggestions();

    explicit Locator(GWidget* parent);

    RefPtr<LocatorTextBox> m_textbox;
    RefPtr<GWindow> m_popup_window;
    RefPtr<GTableView> m_suggestion_view;
};
