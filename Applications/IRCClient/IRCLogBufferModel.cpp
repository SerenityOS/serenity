#include "IRCLogBufferModel.h"
#include "IRCLogBuffer.h"
#include <stdio.h>

IRCLogBufferModel::IRCLogBufferModel(Retained<IRCLogBuffer>&& log_buffer)
    : m_log_buffer(move(log_buffer))
{
}

IRCLogBufferModel::~IRCLogBufferModel()
{
}

int IRCLogBufferModel::row_count() const
{
    return m_log_buffer->count();
}

int IRCLogBufferModel::column_count() const
{
    return 4;
}

String IRCLogBufferModel::column_name(int column) const
{
    switch (column) {
    case Column::Timestamp: return "Time";
    case Column::Prefix: return "@";
    case Column::Name: return "Name";
    case Column::Text: return "Text";
    }
    ASSERT_NOT_REACHED();
}

GTableModel::ColumnMetadata IRCLogBufferModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Timestamp: return { 90, TextAlignment::CenterLeft };
    case Column::Prefix: return { 10, TextAlignment::CenterLeft };
    case Column::Name: return { 70, TextAlignment::CenterRight };
    case Column::Text: return { 400, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCLogBufferModel::data(const GModelIndex& index, Role role) const
{
    auto& entry = m_log_buffer->at(index.row());
    switch (index.column()) {
    case Column::Timestamp: return String::format("%u", entry.timestamp);
    case Column::Prefix: return entry.prefix;
    case Column::Name: return entry.sender;
    case Column::Text: return entry.text;
    }
    ASSERT_NOT_REACHED();
}

void IRCLogBufferModel::update()
{
    did_update();
}

void IRCLogBufferModel::activate(const GModelIndex&)
{
}
