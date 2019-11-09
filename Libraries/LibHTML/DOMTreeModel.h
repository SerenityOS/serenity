#pragma once

#include <LibGUI/GModel.h>

class Document;

class DOMTreeModel final : public GModel {
public:
    static NonnullRefPtr<DOMTreeModel> create(Document& document)
    {
        return adopt(*new DOMTreeModel(document));
    }

    virtual ~DOMTreeModel() override;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual GModelIndex index(int row, int column, const GModelIndex& parent = GModelIndex()) const override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual void update() override;

private:
    explicit DOMTreeModel(Document&);

    NonnullRefPtr<Document> m_document;

    GIcon m_document_icon;
    GIcon m_element_icon;
    GIcon m_text_icon;
};
