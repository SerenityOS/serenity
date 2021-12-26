#pragma once

#include <LibGUI/GTableModel.h>

class GSortingProxyTableModel final : public GTableModel {
public:
    static Retained<GSortingProxyTableModel> create(Retained<GTableModel>&& model) { return adopt(*new GSortingProxyTableModel(move(model))); }
    virtual ~GSortingProxyTableModel() override;

    virtual int row_count() const override;
    virtual int column_count() const override;
    virtual String row_name(int) const override;
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual void activate(const GModelIndex&) override;

    virtual int key_column() const override { return m_key_column; }
    virtual GSortOrder sort_order() const override { return m_sort_order; }
    virtual void set_key_column_and_sort_order(int, GSortOrder) override;

    GModelIndex map_to_target(const GModelIndex&) const;

private:
    explicit GSortingProxyTableModel(Retained<GTableModel>&&);

    GTableModel& target() { return *m_target; }
    const GTableModel& target() const { return *m_target; }

    void resort();

    Retained<GTableModel> m_target;
    Vector<int> m_row_mappings;
    int m_key_column { -1 };
    GSortOrder m_sort_order { GSortOrder::Ascending };
};
