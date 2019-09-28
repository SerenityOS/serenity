#pragma once

#include <AK/String.h>
#include <LibHTML/DOM/Node.h>

class Text final : public Node {
public:
    explicit Text(const String&);
    virtual ~Text() override;

    const String& data() const { return m_data; }

    virtual String tag_name() const override { return "#text"; }

private:
    String m_data;
};
