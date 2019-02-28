#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <LibGUI/GModelIndex.h>

class GTableView;

class GTableModel {
public:
    virtual ~GTableModel();

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

    void register_view(Badge<GTableView>, GTableView&);
    void unregister_view(Badge<GTableView>, GTableView&);

protected:
    GTableModel();

    void for_each_view(Function<void(GTableView&)>);
    void did_update();

private:
    HashTable<GTableView*> m_views;
};
