#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/Painter.h>
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

void GAbstractView::model_notification(const GModelNotification&)
{
}

void GAbstractView::did_update_model()
{
    model_notification(GModelNotification(GModelNotification::ModelUpdated));
}
