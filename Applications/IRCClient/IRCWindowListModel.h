#pragma once

#include <LibGUI/GModel.h>
#include <AK/Function.h>

class IRCClient;
class IRCWindow;

class IRCWindowListModel final : public GModel {
public:
    enum Column {
        Name,
    };

    static Retained<IRCWindowListModel> create(IRCClient& client) { return adopt(*new IRCWindowListModel(client)); }
    virtual ~IRCWindowListModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    Function<void(IRCWindow&)> on_activation;

private:
    explicit IRCWindowListModel(IRCClient&);

    IRCClient& m_client;
};
