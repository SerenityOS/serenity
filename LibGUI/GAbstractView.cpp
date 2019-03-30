#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GPainter.h>
#include <Kernel/KeyCode.h>

GAbstractView::GAbstractView(GWidget* parent)
    : GScrollableWidget(parent)
{
}

GAbstractView::~GAbstractView()
{
}

void GAbstractView::set_model(RetainPtr<GModel>&& model)
{
    if (model.ptr() == m_model.ptr())
        return;
    if (m_model)
        m_model->unregister_view(Badge<GAbstractView>(), *this);
    m_model = move(model);
    if (m_model)
        m_model->register_view(Badge<GAbstractView>(), *this);
    did_update_model();
}

void GAbstractView::model_notification(const GModelNotification& notification)
{
    if (on_model_notification)
        on_model_notification(notification);
}

void GAbstractView::did_update_model()
{
    model_notification(GModelNotification(GModelNotification::ModelUpdated));
}

void GAbstractView::did_update_selection()
{
}
