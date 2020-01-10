#pragma once

#include <AK/Function.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GModelSelection.h>
#include <LibGUI/GScrollableWidget.h>

class GModelEditingDelegate;

class GAbstractView : public GScrollableWidget {
    C_OBJECT(GAbstractView)
    friend class GModel;

public:
    void set_model(RefPtr<GModel>&&);
    GModel* model() { return m_model.ptr(); }
    const GModel* model() const { return m_model.ptr(); }

    GModelSelection& selection() { return m_selection; }
    const GModelSelection& selection() const { return m_selection; }
    void select_all();

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

    Function<void()> on_selection_change;
    Function<void(const GModelIndex&)> on_activation;
    Function<void(const GModelIndex&)> on_selection;
    Function<void(const GModelIndex&, const GContextMenuEvent&)> on_context_menu_request;

    Function<OwnPtr<GModelEditingDelegate>(const GModelIndex&)> aid_create_editing_delegate;

    void notify_selection_changed(Badge<GModelSelection>);

    NonnullRefPtr<Font> font_for_index(const GModelIndex&) const;

protected:
    explicit GAbstractView(GWidget* parent);
    virtual ~GAbstractView() override;

    virtual void did_scroll() override;
    void activate(const GModelIndex&);
    void activate_selected();
    void update_edit_widget_position();

    bool m_editable { false };
    GModelIndex m_edit_index;
    RefPtr<GWidget> m_edit_widget;
    Rect m_edit_widget_content_rect;

private:
    RefPtr<GModel> m_model;
    OwnPtr<GModelEditingDelegate> m_editing_delegate;
    GModelSelection m_selection;
    bool m_activates_on_selection { false };
};
