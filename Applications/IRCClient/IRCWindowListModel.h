#pragma once

#include <AK/Function.h>
#include <LibGUI/GModel.h>

class IRCClient;
class IRCWindow;

class IRCWindowListModel final : public GModel {
public:
    enum Column {
        Name,
    };

    static NonnullRefPtr<IRCWindowListModel> create(IRCClient& client) { return adopt(*new IRCWindowListModel(client)); }
    virtual ~IRCWindowListModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit IRCWindowListModel(IRCClient&);

    IRCClient& m_client;
};
