#pragma once

class GModelIndex {
public:
    GModelIndex() { }
    GModelIndex(int row, int column)
        : m_row(row)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_row != -1 && m_column != -1; }
    int row() const { return m_row; }
    int column() const { return m_column; }

private:
    int m_row { -1 };
    int m_column { -1 };
};
