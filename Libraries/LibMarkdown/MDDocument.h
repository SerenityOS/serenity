#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <LibMarkdown/MDBlock.h>

class MDDocument final {
public:
    String render_to_html() const;
    String render_for_terminal() const;

    bool parse(const StringView&);

private:
    NonnullOwnPtrVector<MDBlock> m_blocks;
};
