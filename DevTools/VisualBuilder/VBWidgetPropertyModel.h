#pragma once

#include <LibGUI/GModel.h>

class VBWidget;
class VBProperty;

class VBWidgetPropertyModel : public GModel {
public:
    enum Column {
        Name = 0,
        Value,
        __Count
    };

    static Retained<VBWidgetPropertyModel> create(VBWidget& widget) { return adopt(*new VBWidgetPropertyModel(widget)); }
    virtual ~VBWidgetPropertyModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override { return Column::__Count; }
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override { did_update(); }
    virtual bool is_editable(const GModelIndex&) const override;
    virtual void set_data(const GModelIndex&, const GVariant&) override;

private:
    explicit VBWidgetPropertyModel(VBWidget&);

    VBWidget& m_widget;
};
