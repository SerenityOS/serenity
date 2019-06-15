#include <LibHTML/Layout/LayoutText.h>
#include <ctype.h>

LayoutText::LayoutText(const Text& text)
    : LayoutNode(&text)
{
}

LayoutText::~LayoutText()
{
}

static bool is_all_whitespace(const String& string)
{
    for (int i = 0; i < string.length(); ++i) {
        if (!isspace(string[i]))
            return false;
    }
    return true;
}

const String& LayoutText::text() const
{
    static String one_space = " ";
    if (is_all_whitespace(node().data()))
        return one_space;
    return node().data();
}
