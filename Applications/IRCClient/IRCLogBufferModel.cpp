#include "IRCLogBufferModel.h"
#include "IRCLogBuffer.h"
#include <LibDraw/Font.h>
#include <stdio.h>
#include <time.h>

IRCLogBufferModel::IRCLogBufferModel(NonnullRefPtr<IRCLogBuffer>&& log_buffer)
    : m_log_buffer(move(log_buffer))
{
}

IRCLogBufferModel::~IRCLogBufferModel()
{
}

int IRCLogBufferModel::row_count(const GModelIndex&) const
{
    return m_log_buffer->count();
}

int IRCLogBufferModel::column_count(const GModelIndex&) const
{
    return Column::__Count;
}

String IRCLogBufferModel::column_name(int column) const
{
    switch (column) {
    case Column::Timestamp:
        return "Time";
    case Column::Name:
        return "Name";
    case Column::Text:
        return "Text";
    }
    ASSERT_NOT_REACHED();
}

GModel::ColumnMetadata IRCLogBufferModel::column_metadata(int column) const
{
    switch (column) {
    case Column::Timestamp:
        return { 60, TextAlignment::CenterLeft };
    case Column::Name:
        return { 70, TextAlignment::CenterRight, &Font::default_bold_font() };
    case Column::Text:
        return { 800, TextAlignment::CenterLeft };
    }
    ASSERT_NOT_REACHED();
}

GVariant IRCLogBufferModel::data(const GModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        auto& entry = m_log_buffer->at(index.row());
        switch (index.column()) {
        case Column::Timestamp: {
            auto* tm = localtime(&entry.timestamp);
            return String::format("%02u:%02u:%02u", tm->tm_hour, tm->tm_min, tm->tm_sec);
        }
        case Column::Name:
            if (entry.sender.is_empty())
                return String::empty();
            return String::format("<%c%s>", entry.prefix ? entry.prefix : ' ', entry.sender.characters());
        case Column::Text:
            return entry.text;
        }
    }
    if (role == Role::ForegroundColor) {
        if (index.column() == Column::Timestamp)
            return Color(Color::MidGray);
        if (index.column() == Column::Text)
            return m_log_buffer->at(index.row()).color;
    }
    return {};
}

void IRCLogBufferModel::update()
{
    did_update();
}
