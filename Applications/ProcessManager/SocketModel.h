#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class SocketModel final : public GModel {
public:
    enum Column {
        PeerAddress,
        PeerPort,
        LocalAddress,
        LocalPort,
        State,
        AckNumber,
        SeqNumber,
        PacketsIn,
        PacketsOut,
        BytesIn,
        BytesOut,
        __Count
    };

    static NonnullRefPtr<SocketModel> create() { return adopt(*new SocketModel); }

    virtual ~SocketModel() override {}

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    SocketModel() {}

    JsonArray m_sockets;
};
