#pragma once

#include <AK/AKString.h>
#include <LibGUI/GModelIndex.h>

class GTableModel {
public:
    GTableModel() { }
    virtual ~GTableModel() { }

    virtual int row_count() const = 0;
    virtual int column_count() const = 0;
    virtual String row_name(int) const { return { }; }
    virtual String column_name(int) const { return { }; }
    virtual int column_width(int) const { return 0; }
    virtual String data(int row, int column) const = 0;
    virtual void set_selected_index(GModelIndex) { }
    virtual GModelIndex selected_index() const { return GModelIndex(); }
    virtual void update() = 0;

    bool is_valid(GModelIndex index) const
    {
        return index.row() >= 0 && index.row() < row_count() && index.column() >= 0 && index.column() < column_count();
    }
};
