#pragma once

#include <LibGUI/GWidget.h>

class LocatorTextBox;
class GModelIndex;
class GTableView;

class Locator final : public GWidget {
    C_OBJECT(Locator)
public:
    virtual ~Locator() override;

    void open();
    void close();

private:
    void update_suggestions();
    void open_suggestion(const GModelIndex&);

    explicit Locator(GWidget* parent);

    RefPtr<LocatorTextBox> m_textbox;
    RefPtr<GWindow> m_popup_window;
    RefPtr<GTableView> m_suggestion_view;
};
