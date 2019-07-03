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
    String string(str);
    bool ok;
    int as_int = string.to_int(ok);
    if (ok)
        return adopt(*new LengthStyleValue(Length(as_int, Length::Type::Absolute)));
    unsigned as_uint = string.to_uint(ok);
    if (ok)
        return adopt(*new LengthStyleValue(Length(as_uint, Length::Type::Absolute)));
    if (string == "auto")
        return adopt(*new LengthStyleValue(Length()));
    return adopt(*new StringStyleValue(str));
}
