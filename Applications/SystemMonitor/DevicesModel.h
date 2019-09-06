#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/GModel.h>

class DevicesModel final : public GModel {
public:
    enum Column {
        Device = 0,
        Major,
        Minor,
        ClassName,
        Type,
        __Count
    };

    virtual ~DevicesModel() override;
    static NonnullRefPtr<DevicesModel> create();

    virtual int row_count(const GModelIndex&) const override;
    virtual int column_count(const GModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual ColumnMetadata column_metadata(int column) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    DevicesModel();

    struct DeviceInfo {
        String path;
        unsigned major;
        unsigned minor;
        String class_name;
        enum Type {
            Block,
            Character
        };
        Type type;
    };

    Vector<DeviceInfo> m_devices;
};
