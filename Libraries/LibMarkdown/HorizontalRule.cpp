/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibMarkdown/HorizontalRule.h>

namespace Markdown {

String HorizontalRule::render_to_html() const
{
    return "<hr>\n";
}

String HorizontalRule::render_for_terminal(size_t view_width) const
{
    StringBuilder builder(view_width + 1);
    for (size_t i = 0; i < view_width; ++i)
        builder.append('-');
    builder.append("\n\n");
    return builder.to_string();
}

OwnPtr<HorizontalRule> HorizontalRule::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return nullptr;

    const StringView& line = *lines;

    if (line.length() < 3)
        return nullptr;
    if (!line.starts_with('-') && !line.starts_with('_') && !line.starts_with('*'))
        return nullptr;

    auto first_character = line.characters_without_null_termination()[0];
    for (auto ch : line) {
        if (ch != first_character)
            return nullptr;
    }

    ++lines;
    return make<HorizontalRule>();
}

}
