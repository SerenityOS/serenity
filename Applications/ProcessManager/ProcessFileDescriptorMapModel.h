#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class ProcessFileDescriptorMapModel final : public GModel {
public:
    enum Column {
        FileDescriptor,
        Path,
        __Count
    };

    ProcessFileDescriptorMapModel() {}
    virtual ~ProcessFileDescriptorMapModel() override {}

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

    void set_pid(pid_t);

private:
    JsonArray m_process_fds;
    int m_pid { -1 };
};
