#pragma once

#include <AK/JsonValue.h>
#include <LibGUI/GModel.h>

class RemoteObject;

class RemoteObjectPropertyModel final : public GModel {
public:
    virtual ~RemoteObjectPropertyModel() override {}
    static NonnullRefPtr<RemoteObjectPropertyModel> create(RemoteObject& object)
    {
        return adopt(*new RemoteObjectPropertyModel(object));
    }

    enum Column {
        Name,
        Value,
        __Count,
    };

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit RemoteObjectPropertyModel(RemoteObject&);

    RemoteObject& m_object;
    struct NameAndValue {
        JsonValue name;
        JsonValue value;
    };
    Vector<NameAndValue> m_properties;
};
