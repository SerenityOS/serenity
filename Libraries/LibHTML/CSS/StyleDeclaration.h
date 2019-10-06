#pragma once

#include <AK/String.h>
#include <LibHTML/CSS/StyleValue.h>

struct StyleProperty {
    String name;
    NonnullRefPtr<StyleValue> value;
    bool important { false };
};

class StyleDeclaration : public RefCounted<StyleDeclaration> {
public:
    static NonnullRefPtr<StyleDeclaration> create(Vector<StyleProperty>&& properties)
    {
        return adopt(*new StyleDeclaration(move(properties)));
    }

    ~StyleDeclaration();

    const Vector<StyleProperty>& properties() const { return m_properties; }

public:
    explicit StyleDeclaration(Vector<StyleProperty>&&);

    Vector<StyleProperty> m_properties;
};
