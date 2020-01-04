#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/GModel.h>

class StyleProperties;

class StylePropertiesModel final : public GModel {
public:
    enum Column {
        PropertyName,
        PropertyValue,
        __Count
    };

    static NonnullRefPtr<StylePropertiesModel> create(const StyleProperties& properties) { return adopt(*new StylePropertiesModel(properties)); }

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit StylePropertiesModel(const StyleProperties& properties);
    const StyleProperties& properties() const { return *m_properties; }

    NonnullRefPtr<StyleProperties> m_properties;

    struct Value {
        String name;
        String value;
    };
    Vector<Value> m_values;
};
