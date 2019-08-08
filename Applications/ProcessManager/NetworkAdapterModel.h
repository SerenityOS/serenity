#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class NetworkAdapterModel final : public GModel {
public:
    enum Column {
        Name,
        ClassName,
        MacAddress,
        IpAddress,
        PacketsIn,
        PacketsOut,
        BytesIn,
        BytesOut,
        __Count
    };

    static NonnullRefPtr<NetworkAdapterModel> create() { return adopt(*new NetworkAdapterModel); }
    virtual ~NetworkAdapterModel() override {}

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    NetworkAdapterModel() {}
    JsonArray m_netadapters;
};
