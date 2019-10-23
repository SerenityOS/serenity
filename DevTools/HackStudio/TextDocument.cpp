#include "TextDocument.h"
#include <LibCore/CFile.h>
#include <string.h>

const ByteBuffer& TextDocument::contents() const
{
    if (m_contents.is_null()) {
        auto file = CFile::construct(m_name);
        if (file->open(CFile::ReadOnly))
            m_contents = file->read_all();
    }
    return m_contents;
}

Vector<int> TextDocument::find(const StringView& needle) const
{
    // NOTE: This forces us to load the contents if we hadn't already.
    contents();

    Vector<int> matching_line_numbers;

    String needle_as_string(needle);

    int line_index = 0;
    int start_of_line = 0;
    for (int i = 0; i < m_contents.size(); ++i) {
        char ch = m_contents[i];
        if (ch == '\n') {
            // FIXME: Please come back here and do this the good boy way.
            String line(StringView(m_contents.data() + start_of_line, i - start_of_line));
            auto* found = strstr(line.characters(), needle_as_string.characters());
            if (found)
                matching_line_numbers.append(line_index + 1);
            ++line_index;
            start_of_line = i + 1;
            continue;
        }
    }

    return matching_line_numbers;
}
