#pragma once

#include <AK/JsonArray.h>
#include <LibGUI/GModel.h>

class GJsonArrayModel final : public GModel {
public:
    struct FieldSpec {
        String json_field_name;
        String column_name;
        TextAlignment text_alignment;
    };

    static NonnullRefPtr<GJsonArrayModel> create(const String& json_path, Vector<FieldSpec>&& fields)
    {
        return adopt(*new GJsonArrayModel(json_path, move(fields)));
    }

    virtual ~GJsonArrayModel() override {}

    virtual int row_count(const GModelIndex& = GModelIndex()) const override { return m_array.size(); }
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return m_fields.size(); }
    virtual String column_name(int column) const override { return m_fields[column].column_name; }
    virtual ColumnMetadata column_metadata(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    GJsonArrayModel(const String& json_path, Vector<FieldSpec>&& fields)
        : m_json_path(json_path)
        , m_fields(move(fields))
    {
    }

    String m_json_path;
    Vector<FieldSpec> m_fields;
    JsonArray m_array;
};
