#pragma once

#include <LibGUI/GTableModel.h>

class IRCLogBuffer;

class IRCLogBufferModel final : public GTableModel {
public:
    enum Column {
        Timestamp = 0,
        Prefix,
        Name,
        Text,
    };

    explicit IRCLogBufferModel(Retained<IRCLogBuffer>&&);
    virtual ~IRCLogBufferModel() override;

    virtual int row_count() const override;
    virtual int column_count() const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual void activate(const GModelIndex&) override;

private:
    Retained<IRCLogBuffer> m_log_buffer;
};
