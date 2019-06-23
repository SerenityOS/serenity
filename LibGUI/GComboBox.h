#pragma once

#include <LibGUI/GListView.h>
#include <LibGUI/GWidget.h>

class GButton;
class GTextEditor;

class GComboBox : public GWidget {
public:
    explicit GComboBox(GWidget* parent = nullptr);
    virtual ~GComboBox() override;

    String text() const;
    void set_text(const String&);

    void open();
    void close();

    GModel* model() { return m_list_view->model(); }
    const GModel* model() const { return m_list_view->model(); }
    void set_model(NonnullRefPtr<GModel>);

    Function<void(const String&)> on_change;
    Function<void()> on_return_pressed;

    virtual const char* class_name() const override { return "GComboBox"; }

protected:
    virtual void resize_event(GResizeEvent&) override;

private:
    GTextEditor* m_editor { nullptr };
    GButton* m_open_button { nullptr };
    GWindow* m_list_window { nullptr };
    GListView* m_list_view { nullptr };
};
