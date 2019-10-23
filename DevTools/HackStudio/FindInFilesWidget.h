#pragma once

#include <LibGUI/GWidget.h>

class GButton;
class GListView;
class GTextBox;

class FindInFilesWidget final : public GWidget {
    C_OBJECT(FindInFilesWidget)
public:
    virtual ~FindInFilesWidget() override {}

    void focus_textbox_and_select_all();

private:
    explicit FindInFilesWidget(GWidget* parent);

    RefPtr<GTextBox> m_textbox;
    RefPtr<GButton> m_button;
    RefPtr<GListView> m_result_view;
};
