#pragma once

#include <LibGUI/GTableModel.h>
#include <AK/Function.h>

class IRCChannel;

class IRCChannelMemberListModel final : public GTableModel {
public:
    enum Column { Name };
    static Retained<IRCChannelMemberListModel> create(IRCChannel& channel) { return adopt(*new IRCChannelMemberListModel(channel)); }
    virtual ~IRCChannelMemberListModel() override;

    virtual int row_count() const override;
    virtual int column_count() const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual void activate(const GModelIndex&) override;

    Function<void(const String&)> on_activation;

private:
    explicit IRCChannelMemberListModel(IRCChannel&);

    IRCChannel& m_channel;
};
