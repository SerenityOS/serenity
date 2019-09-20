#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibGUI/GModel.h>

class ManualModel final : public GModel {
public:
    static NonnullRefPtr<ManualModel> create()
    {
        return adopt(*new ManualModel);
    }

    virtual ~ManualModel() override {};

    String page_path(const GModelIndex&) const;
    String page_and_section(const GModelIndex&) const;

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual GModelIndex parent_index(const GModelIndex&) const override;
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& parent = GModelIndex()) const override;

private:
    ManualModel();

    GIcon m_section_icon;
    GIcon m_page_icon;
};
