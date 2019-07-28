#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class ProcessMemoryMapModel final : public GModel {
public:
    enum Column {
        Address,
        Size,
        AmountResident,
        Access,
        Name,
        __Count
    };

    ProcessMemoryMapModel() {}
    virtual ~ProcessMemoryMapModel() override {}

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    void set_pid(pid_t);

private:
    JsonArray m_process_vm;
    int m_pid { -1 };
};
