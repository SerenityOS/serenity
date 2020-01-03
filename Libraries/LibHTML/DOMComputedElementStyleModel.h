#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/GModel.h>

class Element;

class DOMComputedElementStyleModel final : public GModel {
public:
    enum Column {
        PropertyName,
        PropertyValue,
        __Count
    };

    static NonnullRefPtr<DOMComputedElementStyleModel> create(const Element& element) { return adopt(*new DOMComputedElementStyleModel(element)); }

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;

private:
    explicit DOMComputedElementStyleModel(const Element&);
    const Element& element() const { return *m_element; }

    NonnullRefPtr<Element> m_element;

    struct Value {
        String name;
        String value;
    };
    Vector<Value> m_values;
};
