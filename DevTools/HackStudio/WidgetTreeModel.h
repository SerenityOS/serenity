#pragma once

#include <LibGUI/GModel.h>
#include <LibGUI/GPainter.h>

class WidgetTreeModel final : public GModel {
public:
    static NonnullRefPtr<WidgetTreeModel> create(GWidget& root) { return adopt(*new WidgetTreeModel(root)); }
    virtual ~WidgetTreeModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual GModelIndex index(int row, int column, const GModelIndex& parent = GModelIndex()) const override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual void update() override;

private:
    explicit WidgetTreeModel(GWidget&);

    NonnullRefPtr<GWidget> m_root;
    GIcon m_widget_icon;
};
