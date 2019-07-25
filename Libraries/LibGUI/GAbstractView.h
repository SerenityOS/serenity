#pragma once

#include <AK/Function.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GScrollableWidget.h>

class GModelEditingDelegate;

class GAbstractView : public GScrollableWidget {
    C_OBJECT(GAbstractView)
    friend class GModel;
public:
    explicit GAbstractView(GWidget* parent);
    virtual ~GAbstractView() override;

    void set_model(RefPtr<GModel>&&);
    GModel* model() { return m_model.ptr(); }
    const GModel* model() const { return m_model.ptr(); }

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model();
    virtual void did_update_selection();

    virtual Rect content_rect(const GModelIndex&) const { return {}; }
    void begin_editing(const GModelIndex&);
    void stop_editing();

    void set_activates_on_selection(bool b) { m_activates_on_selection = b; }
    bool activates_on_selection() const { return m_activates_on_selection; }

    Function<void(const GModelIndex&)> on_activation;
    Function<void(const GModelIndex&)> on_selection;
    Function<void(const GModelIndex&, const GContextMenuEvent&)> on_context_menu_request;
    Function<void(const GModelNotification&)> on_model_notification;

    Function<OwnPtr<GModelEditingDelegate>(const GModelIndex&)> aid_create_editing_delegate;

protected:
    virtual void model_notification(const GModelNotification&);
    virtual void did_scroll() override;
    void activate(const GModelIndex&);
    void update_edit_widget_position();

    bool m_editable { false };
    GModelIndex m_edit_index;
    GWidget* m_edit_widget { nullptr };
    Rect m_edit_widget_content_rect;

private:
    RefPtr<GModel> m_model;
    OwnPtr<GModelEditingDelegate> m_editing_delegate;
    bool m_activates_on_selection { false };
};
