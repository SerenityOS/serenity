#include <Kernel/KeyCode.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTextBox.h>

GAbstractView::GAbstractView(GWidget* parent)
    : GScrollableWidget(parent)
{
}

GAbstractView::~GAbstractView()
{
    delete m_edit_widget;
}

void GAbstractView::set_model(RefPtr<GModel>&& model)
{
    if (model == m_model)
        return;
    if (m_model)
        m_model->unregister_view({}, *this);
    m_model = move(model);
    if (m_model)
        m_model->register_view({}, *this);
    did_update_model();
}

void GAbstractView::model_notification(const GModelNotification& notification)
{
    if (on_model_notification)
        on_model_notification(notification);
}

void GAbstractView::did_update_model()
{
    if (!model() || model()->selected_index() != m_edit_index)
        stop_editing();
    model_notification(GModelNotification(GModelNotification::ModelUpdated));
}

void GAbstractView::did_update_selection()
{
    if (!model() || model()->selected_index() != m_edit_index)
        stop_editing();
}

void GAbstractView::did_scroll()
{
    update_edit_widget_position();
}

void GAbstractView::update_edit_widget_position()
{
    if (!m_edit_widget)
        return;
    m_edit_widget->set_relative_rect(m_edit_widget_content_rect.translated(-horizontal_scrollbar().value(), -vertical_scrollbar().value()));
}

void GAbstractView::begin_editing(const GModelIndex& index)
{
    ASSERT(is_editable());
    ASSERT(model());
    if (m_edit_index == index)
        return;
    if (!model()->is_editable(index))
        return;
    if (m_edit_widget)
        delete m_edit_widget;
    m_edit_index = index;
    m_edit_widget = new GTextBox(this);
    m_edit_widget->move_to_back();
    m_edit_widget->set_text(model()->data(index, GModel::Role::Display).to_string());
    m_edit_widget_content_rect = content_rect(index);
    update_edit_widget_position();
    m_edit_widget->set_focus(true);
    m_edit_widget->on_return_pressed = [this] {
        ASSERT(model());
        model()->set_data(m_edit_index, m_edit_widget->text());
        stop_editing();
    };
}

void GAbstractView::stop_editing()
{
    m_edit_index = {};
    delete m_edit_widget;
    m_edit_widget = nullptr;
}

void GAbstractView::activate(const GModelIndex& index)
{
    if (on_activation)
        on_activation(index);
}
