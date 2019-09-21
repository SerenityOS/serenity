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

    int model_column() const { return m_list_view->model_column(); }
    void set_model_column(int column) { m_list_view->set_model_column(column); }

    Function<void(const String&, const GModelIndex&)> on_change;
    Function<void()> on_return_pressed;

protected:
    virtual void resize_event(GResizeEvent&) override;

private:
    ObjectPtr<GTextEditor> m_editor;
    GButton* m_open_button { nullptr };
    GWindow* m_list_window { nullptr };
    ObjectPtr<GListView> m_list_view;
    bool m_only_allow_values_from_model { false };
};
