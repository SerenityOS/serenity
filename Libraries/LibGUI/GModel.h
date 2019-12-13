#pragma once

#include <AK/String.h>
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

class GModel : public RefCounted<GModel> {
public:
    struct ColumnMetadata {
        int preferred_width { 0 };
        TextAlignment text_alignment { TextAlignment::CenterLeft };
        const Font* font { nullptr };
	enum class Sortable { False, True };
	Sortable sortable { Sortable::True };
    };

    enum class Role {
        Display,
        Sort,
        Custom,
        ForegroundColor,
        BackgroundColor,
        Icon,
        Font,
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
    virtual int tree_column() const { return 0; }

    bool is_valid(const GModelIndex& index) const
    {
        return index.row() >= 0 && index.row() < row_count() && index.column() >= 0 && index.column() < column_count();
    }

    virtual int key_column() const { return -1; }
    virtual GSortOrder sort_order() const { return GSortOrder::None; }
    virtual void set_key_column_and_sort_order(int, GSortOrder) {}

    void register_view(Badge<GAbstractView>, GAbstractView&);
    void unregister_view(Badge<GAbstractView>, GAbstractView&);

    Function<void()> on_update;

protected:
    GModel();

    void for_each_view(Function<void(GAbstractView&)>);
    void did_update();

    GModelIndex create_index(int row, int column, const void* data = nullptr) const;

private:
    HashTable<GAbstractView*> m_views;
};

inline GModelIndex GModelIndex::parent() const
{
    return m_model ? m_model->parent_index(*this) : GModelIndex();
}
