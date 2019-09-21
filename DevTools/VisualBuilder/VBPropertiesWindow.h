#pragma once

#include <LibGUI/GWindow.h>

class GTableView;
class GTextBox;

class VBPropertiesWindow final : public GWindow {
public:
    VBPropertiesWindow();
    virtual ~VBPropertiesWindow() override;

    GTableView& table_view() { return *m_table_view; }
    const GTableView& table_view() const { return *m_table_view; }

private:
    ObjectPtr<GTableView> m_table_view;
};
