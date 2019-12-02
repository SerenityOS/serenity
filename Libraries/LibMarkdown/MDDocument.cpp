#include <AK/StringBuilder.h>
#include <LibMarkdown/MDCodeBlock.h>
#include <LibMarkdown/MDDocument.h>
#include <LibMarkdown/MDHeading.h>
#include <LibMarkdown/MDList.h>
#include <LibMarkdown/MDParagraph.h>

String MDDocument::render_to_html() const
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head></head>\n");
    builder.append("<body>\n");

    for (auto& block : m_blocks) {
        auto s = block.render_to_html();
        builder.append(s);
    }

    builder.append("</body>\n");
    builder.append("</html>\n");
    return builder.build();
}

String MDDocument::render_for_terminal() const
{
    StringBuilder builder;

    for (auto& block : m_blocks) {
        auto s = block.render_for_terminal();
        builder.append(s);
    }

    return builder.build();
}

template<typename Block>
static bool helper(Vector<StringView>::ConstIterator& lines, NonnullOwnPtrVector<MDBlock>& blocks)
{
    NonnullOwnPtr<Block> block = make<Block>();
    bool success = block->parse(lines);
    if (!success)
        return false;
    blocks.append(move(block));
    return true;
}

bool MDDocument::parse(const StringView& str)
{
    const Vector<StringView> lines_vec = str.lines();
    auto lines = lines_vec.begin();

    while (true) {
        if (lines.is_end())
            return true;

        if ((*lines).is_empty()) {
            ++lines;
            continue;
        }

        bool any = helper<MDList>(lines, m_blocks) || helper<MDParagraph>(lines, m_blocks) || helper<MDCodeBlock>(lines, m_blocks) || helper<MDHeading>(lines, m_blocks);

        if (!any)
            return false;
    }
}
