#pragma once

#include <AK/Function.h>
#include <LibGUI/GModel.h>

class IRCChannel;

class IRCChannelMemberListModel final : public GModel {
public:
    enum Column {
        Name
    };
    static Retained<IRCChannelMemberListModel> create(IRCChannel& channel) { return adopt(*new IRCChannelMemberListModel(channel)); }
    virtual ~IRCChannelMemberListModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit IRCChannelMemberListModel(IRCChannel&);

    IRCChannel& m_channel;
};
