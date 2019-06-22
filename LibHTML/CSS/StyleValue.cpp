#include "StyleValue.h"

StyleValue::StyleValue(Type type)
    : m_type(type)
{
}

StyleValue::~StyleValue()
{
}

NonnullRefPtr<StyleValue> StyleValue::parse(const StringView& str)
{
    return adopt(*new PrimitiveStyleValue(str));
}
