#pragma once

#include <LibGUI/GModel.h>
#include <LibGUI/GScrollableWidget.h>
#include <AK/Function.h>

class GTextBox;

class GAbstractView : public GScrollableWidget {
    friend class GModel;
public:
    explicit GAbstractView(GWidget* parent);
    virtual ~GAbstractView() override;

    void set_model(RetainPtr<GModel>&&);
    GModel* model() { return m_model.ptr(); }
    const GModel* model() const { return m_model.ptr(); }

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model();
    virtual void did_update_selection();

    Function<void(const GModelNotification&)> on_model_notification;

    virtual const char* class_name() const override { return "GAbstractView"; }

protected:
    virtual void model_notification(const GModelNotification&);

    bool m_editable { false };
    GModelIndex m_edit_index;
    GTextBox* m_edit_widget { nullptr };

private:
    RetainPtr<GModel> m_model;
};
