#pragma once

#include <LibGUI/GTableModel.h>

class IRCClient;

class IRCClientWindowListModel final : public GTableModel {
public:
    enum Column {
        Name,
    };

    explicit IRCClientWindowListModel(IRCClient&);
    virtual ~IRCClientWindowListModel() override;

    virtual int row_count() const override;
    virtual int column_count() const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual void activate(const GModelIndex&) override;

private:
    IRCClient& m_client;
};
