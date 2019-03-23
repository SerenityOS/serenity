#pragma once

#include <LibGUI/GModel.h>
#include <LibGUI/GScrollableWidget.h>
#include <AK/Function.h>

class GAbstractView : public GScrollableWidget {
    friend class GModel;
public:
    explicit GAbstractView(GWidget* parent);
    virtual ~GAbstractView() override;

    void set_model(RetainPtr<GModel>&&);
    GModel* model() { return m_model.ptr(); }
    const GModel* model() const { return m_model.ptr(); }

    void scroll_into_view(const GModelIndex&, Orientation);

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model();

    Function<void(const GModelNotification&)> on_model_notification;

    virtual const char* class_name() const override { return "GAbstractView"; }

protected:
    virtual void model_notification(const GModelNotification&);

private:
    RetainPtr<GModel> m_model;
};
