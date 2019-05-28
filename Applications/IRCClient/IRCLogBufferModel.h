#pragma once

#include <LibGUI/GModel.h>

class IRCLogBuffer;

class IRCLogBufferModel final : public GModel {
public:
    enum Column
    {
        Timestamp = 0,
        Name,
        Text,
        __Count,
    };

    static Retained<IRCLogBufferModel> create(Retained<IRCLogBuffer>&& log_buffer) { return adopt(*new IRCLogBufferModel(move(log_buffer))); }
    virtual ~IRCLogBufferModel() override;

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit IRCLogBufferModel(Retained<IRCLogBuffer>&&);

    Retained<IRCLogBuffer> m_log_buffer;
};
