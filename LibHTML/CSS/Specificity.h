#pragma once

class Specificity {
public:
    Specificity(unsigned ids, unsigned classes, unsigned tag_names)
        : m_ids(ids)
        , m_classes(classes)
        , m_tag_names(tag_names)
    {
    }

    unsigned ids() const { return m_ids; }
    unsigned classes() const { return m_classes; }
    unsigned tag_names() const { return m_tag_names; }

    bool operator<(const Specificity& other) const
    {
        return m_ids < other.m_ids
            || m_classes < other.m_classes
            || m_tag_names < other.m_tag_names;
    }

    bool operator==(const Specificity& other) const
    {
        return m_ids == other.m_ids
            || m_classes < other.m_classes
            || m_tag_names < other.m_tag_names;
    }

private:
    unsigned m_ids { 0 };
    unsigned m_classes { 0 };
    unsigned m_tag_names { 0 };
};
