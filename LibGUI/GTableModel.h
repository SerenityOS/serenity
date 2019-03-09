#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <LibGUI/GModelIndex.h>
#include <LibGUI/GVariant.h>
#include <SharedGraphics/TextAlignment.h>

class GTableView;

enum class GSortOrder { None, Ascending, Descending };

class GModelNotification {
public:
    enum Type {
        Invalid = 0,
        ModelUpdated,
    };

    explicit GModelNotification(Type type, const GModelIndex& index = GModelIndex())
        : m_type(type)
        , m_index(index)
    {
    }

    Type type() const { return m_type; }
    GModelIndex index() const { return m_index; }

private:
    Type m_type { Invalid };
    GModelIndex m_index;
};

class GTableModel {
public:
    struct ColumnMetadata {
        int preferred_width { 0 };
        TextAlignment text_alignment { TextAlignment::CenterLeft };
    };

    virtual ~GTableModel();

    virtual int row_count() const = 0;
    virtual int column_count() const = 0;
    virtual String row_name(int) const { return { }; }
    virtual String column_name(int) const { return { }; }
    virtual ColumnMetadata column_metadata(int) const { return { }; }
    virtual GVariant data(const GModelIndex&) const = 0;
    virtual void update() = 0;
    virtual void activate(const GModelIndex&) { }

    bool is_valid(const GModelIndex& index) const
    {
        return index.row() >= 0 && index.row() < row_count() && index.column() >= 0 && index.column() < column_count();
    }

    void set_selected_index(const GModelIndex& index) { m_selected_index = index; }
    GModelIndex selected_index() const { return m_selected_index; }

    virtual int key_column() const { return -1; }
    virtual GSortOrder sort_order() const { return GSortOrder::None; }
    virtual void set_key_column_and_sort_order(int, GSortOrder) { }

    void register_view(Badge<GTableView>, GTableView&);
    void unregister_view(Badge<GTableView>, GTableView&);

    Function<void(GTableModel&)> on_model_update;

protected:
    GTableModel();

    void for_each_view(Function<void(GTableView&)>);
    void did_update();

private:
    HashTable<GTableView*> m_views;
    GModelIndex m_selected_index;
};
