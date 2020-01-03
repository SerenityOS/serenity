#include <LibGUI/GWidget.h>

class Document;
class GTableView;
class GTreeView;

class InspectorWidget final : public GWidget {
    C_OBJECT(InspectorWidget)
public:
    virtual ~InspectorWidget();

    void set_document(Document*);

private:
    explicit InspectorWidget(GWidget* parent);

    RefPtr<GTreeView> m_dom_tree_view;
    RefPtr<GTableView> m_style_table_view;
    RefPtr<GTableView> m_computed_style_table_view;
    RefPtr<Document> m_document;
};
