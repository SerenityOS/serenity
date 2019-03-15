#include "IRCLogBufferModel.h"
#include "IRCLogBuffer.h"
#include <stdio.h>
#include <time.h>

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
    case Column::Timestamp: return { 60, TextAlignment::CenterLeft };
    case Column::Prefix: return { 10, TextAlignment::CenterLeft };
    case Column::Name: return { 70, TextAlignment::CenterRight };
    case Column::Text: return { 800, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCLogBufferModel::data(const GModelIndex& index, Role) const
{
    auto& entry = m_log_buffer->at(index.row());
    switch (index.column()) {
    case Column::Timestamp: {
        auto* tm = localtime(&entry.timestamp);
        return String::format("%02u:%02u:%02u", tm->tm_hour, tm->tm_min, tm->tm_sec);
    }
    case Column::Prefix: {
        if (!entry.prefix)
            return String("");
        return String(&entry.prefix, 1);
    }
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
