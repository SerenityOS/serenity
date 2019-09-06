#pragma once

#include <AK/String.h>
#include <AK/LogStream.h>

class GModel;

class GModelIndex {
    friend class GModel;

public:
    GModelIndex() {}

    bool is_valid() const { return m_row != -1 && m_column != -1; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    void* internal_data() const { return m_internal_data; }

    GModelIndex parent() const;

    bool operator==(const GModelIndex& other) const
    {
        return m_model == other.m_model && m_row == other.m_row && m_column == other.m_column && m_internal_data == other.m_internal_data;
    }

    bool operator!=(const GModelIndex& other) const
    {
        return !(*this == other);
    }

private:
    GModelIndex(const GModel& model, int row, int column, void* internal_data)
        : m_model(&model)
        , m_row(row)
        , m_column(column)
        , m_internal_data(internal_data)
    {
    }

    const GModel* m_model { nullptr };
    int m_row { -1 };
    int m_column { -1 };
    void* m_internal_data { nullptr };
};

inline const LogStream& operator<<(const LogStream& stream, const GModelIndex& value)
{
    return stream << String::format("GModelIndex(%d,%d)", value.row(), value.column());
}
