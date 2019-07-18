#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/RefCounted.h>
#include <LibGUI/GModelIndex.h>
#include <LibGUI/GVariant.h>
#include <LibDraw/TextAlignment.h>

class Font;
class GAbstractView;

enum class GSortOrder {
    None,
    Ascending,
    Descending
};

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

class GModel : public RefCounted<GModel> {
public:
    struct ColumnMetadata {
        int preferred_width { 0 };
        TextAlignment text_alignment { TextAlignment::CenterLeft };
        const Font* font { nullptr };
    };

    enum class Role {
        Display,
        Sort,
        Custom,
        ForegroundColor,
        BackgroundColor,
        Icon
    };

    virtual ~GModel();

    virtual int row_count(const GModelIndex& = GModelIndex()) const = 0;
    virtual int column_count(const GModelIndex& = GModelIndex()) const = 0;
    virtual String row_name(int) const { return {}; }
    virtual String column_name(int) const { return {}; }
    virtual ColumnMetadata column_metadata(int) const { return {}; }
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const = 0;
    virtual void update() = 0;
    virtual GModelIndex parent_index(const GModelIndex&) const { return {}; }
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& = GModelIndex()) const { return create_index(row, column); }
    virtual GModelIndex sibling(int row, int column, const GModelIndex& parent) const;
    virtual bool is_editable(const GModelIndex&) const { return false; }
    virtual void set_data(const GModelIndex&, const GVariant&) {}

    bool is_valid(const GModelIndex& index) const
    {
        return index.row() >= 0 && index.row() < row_count() && index.column() >= 0 && index.column() < column_count();
    }

    void set_selected_index(const GModelIndex&);
    GModelIndex selected_index() const { return m_selected_index; }

    virtual int key_column() const { return -1; }
    virtual GSortOrder sort_order() const { return GSortOrder::None; }
    virtual void set_key_column_and_sort_order(int, GSortOrder) {}

    void register_view(Badge<GAbstractView>, GAbstractView&);
    void unregister_view(Badge<GAbstractView>, GAbstractView&);

    Function<void(GModel&)> on_model_update;
    Function<void(const GModelIndex&)> on_selection_changed;

protected:
    GModel();

    void for_each_view(Function<void(GAbstractView&)>);
    void did_update();

    GModelIndex create_index(int row, int column, void* data = nullptr) const;

private:
    HashTable<GAbstractView*> m_views;
    GModelIndex m_selected_index;
};

inline GModelIndex GModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : GModelIndex();
}
