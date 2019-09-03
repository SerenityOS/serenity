#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGUI/GModel.h>

template<typename T>
class ItemListModel final : public GModel {
public:
    static NonnullRefPtr<ItemListModel> create(Vector<T>& data) { return adopt(*new ItemListModel<T>(data)); }

    virtual ~ItemListModel() override {}

    virtual int row_count(const GModelIndex&) const override
    {
        return m_data.size();
    }

    virtual int column_count(const GModelIndex&) const override
    {
        return 1;
    }

    virtual String column_name(int) const override
    {
        return "Data";
    }

    virtual ColumnMetadata column_metadata(int) const override
    {
        return { 70, TextAlignment::CenterLeft };
    }

    virtual GVariant data(const GModelIndex& index, Role role = Role::Display) const override
    {
        if (role == Role::Display)
            return m_data.at(index.row());

        return {};
    }

    virtual void update() override
    {
        did_update();
    }

private:
    explicit ItemListModel(Vector<T>& data)
        : m_data(data)
    {
    }

    Vector<T>& m_data;
};
