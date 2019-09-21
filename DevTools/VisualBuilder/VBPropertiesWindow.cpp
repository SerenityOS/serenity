#include "VBPropertiesWindow.h"
#include "VBWidgetPropertyModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GComboBox.h>
#include <LibGUI/GModelEditingDelegate.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWidget.h>

class BoolValuesModel final : public GModel {
public:
    virtual int row_count(const GModelIndex&) const override { return 2; }
    virtual int column_count(const GModelIndex&) const override { return 1; }
    virtual void update() override {}
    virtual GVariant data(const GModelIndex& index, Role role) const override
    {
        if (role != Role::Display)
            return {};
        switch (index.row()) {
        case 0:
            return "false";
        case 1:
            return "true";
        }
        ASSERT_NOT_REACHED();
    }
};

class BoolModelEditingDelegate : public GModelEditingDelegate {
public:
    BoolModelEditingDelegate() {}
    virtual ~BoolModelEditingDelegate() override {}

    virtual RefPtr<GWidget> create_widget() override
    {
        auto combo = GComboBox::construct(nullptr);
        combo->set_only_allow_values_from_model(true);
        combo->set_model(adopt(*new BoolValuesModel));
        combo->on_return_pressed = [this] { commit(); };
        combo->on_change = [this](auto&, auto&) { commit(); };
        return combo;
    }
    virtual GVariant value() const override { return static_cast<const GComboBox*>(widget())->text() == "true"; }
    virtual void set_value(const GVariant& value) override { static_cast<GComboBox*>(widget())->set_text(value.to_string()); }
    virtual void will_begin_editing() override
    {
        auto& combo = *static_cast<GComboBox*>(widget());
        combo.select_all();
        combo.open();
    }
};

VBPropertiesWindow::VBPropertiesWindow()
{
    set_title("Properties");
    set_rect(780, 200, 240, 280);

    auto widget = GWidget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 2, 2, 2, 2 });
    set_main_widget(widget);

    m_table_view = GTableView::construct(widget);
    m_table_view->set_headers_visible(false);
    m_table_view->set_editable(true);

    m_table_view->aid_create_editing_delegate = [this](auto& index) -> OwnPtr<GModelEditingDelegate> {
        if (!m_table_view->model())
            return nullptr;
        auto type_index = m_table_view->model()->index(index.row(), VBWidgetPropertyModel::Column::Type);
        auto type = m_table_view->model()->data(type_index, GModel::Role::Custom).to_int();
        switch ((GVariant::Type)type) {
        case GVariant::Type::Bool:
            return make<BoolModelEditingDelegate>();
        default:
            return make<GStringModelEditingDelegate>();
        }
    };
}

VBPropertiesWindow::~VBPropertiesWindow()
{
}
