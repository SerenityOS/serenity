#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class ThreadCatalogModel final : public GModel {
public:
    enum Column {
        ThreadNumber,
        Text,
        ReplyCount,
        ImageCount,
        __Count,
    };

    static NonnullRefPtr<ThreadCatalogModel> create() { return adopt(*new ThreadCatalogModel); }
    virtual ~ThreadCatalogModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    ThreadCatalogModel();

    JsonArray m_catalog;
};
