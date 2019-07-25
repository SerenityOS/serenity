#pragma once

#include <LibGUI/GListView.h>
#include <LibGUI/GWidget.h>

class GButton;
class GTextEditor;

class GComboBox : public GWidget {
    C_OBJECT(GComboBox)
public:
    explicit GComboBox(GWidget* parent = nullptr);
    virtual ~GComboBox() override;

    String text() const;
    void set_text(const String&);

    void open();
    void close();
    void select_all();

    GModel* model() { return m_list_view->model(); }
    const GModel* model() const { return m_list_view->model(); }
    void set_model(NonnullRefPtr<GModel>);

    bool only_allow_values_from_model() const { return m_only_allow_values_from_model; }
    void set_only_allow_values_from_model(bool);

    Function<void(const String&)> on_change;
    Function<void()> on_return_pressed;

protected:
    virtual void resize_event(GResizeEvent&) override;

private:
    GTextEditor* m_editor { nullptr };
    GButton* m_open_button { nullptr };
    GWindow* m_list_window { nullptr };
    GListView* m_list_view { nullptr };
    bool m_only_allow_values_from_model { false };
};
