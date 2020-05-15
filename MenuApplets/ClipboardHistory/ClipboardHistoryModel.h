#include <AK/Vector.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Model.h>

class ClipboardHistoryModel final : public GUI::Model {
public:
    static NonnullRefPtr<ClipboardHistoryModel> create();

    enum Column {
        Data,
        Type,
        __Count
    };

    virtual ~ClipboardHistoryModel() override;

    const GUI::Clipboard::DataAndType& item_at(int index) const { return m_history_items[index]; }
    void add_item(const GUI::Clipboard::DataAndType& item);

private:
    virtual int row_count(const GUI::ModelIndex&) const override { return m_history_items.size(); }
    virtual String column_name(int) const override;
    virtual GUI::Model::ColumnMetadata column_metadata(int column) const override;
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }
    virtual GUI::Variant data(const GUI::ModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    Vector<GUI::Clipboard::DataAndType> m_history_items;
};
