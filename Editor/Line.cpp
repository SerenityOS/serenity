#include "Line.h"

Chunk::Chunk(const std::string& str)
    : m_data(str)
{
}

Chunk::~Chunk()
{
}

Line::Line(const std::string& str)
{
    m_chunks.push_back(Chunk(str));
}

Line::Line(Line&& other)
    : m_chunks(std::move(other.m_chunks))
{
}

Line::~Line()
{
}

std::string Line::data() const
{
    std::string str;
    for (auto& chunk : m_chunks)
        str += chunk.data();
    return str;
}

void Line::append(const std::string& text)
{
    m_chunks.push_back(Chunk(text));
}

void Line::prepend(const std::string& text)
{
    m_chunks.push_front(Chunk(text));
}

void Line::insert(size_t index, const std::string& text)
{
    if (index == 0) {
        prepend(text);
        return;
    }

    if (index == length()) {
        append(text);
        return;
    }

    auto chunk_address = chunk_index_for_position(index);
    auto chunk_index = std::get<0>(chunk_address);
    auto& chunk = m_chunks[chunk_index];
    auto index_in_chunk = std::get<1>(chunk_address);

    static FILE* f = fopen("log", "a");
    fprintf(f, "#Column:%zu, Chunk:%zu, Index:%zu\n", index, chunk_index, index_in_chunk);

    auto left_string = chunk.data().substr(0, index_in_chunk);
    auto right_string = chunk.data().substr(index_in_chunk, chunk.length() - index_in_chunk);

    fprintf(f, "#{\"%s\", \"%s\", \"%s\"}\n", left_string.c_str(), text.c_str(), right_string.c_str());

    Chunk left_chunk { left_string };
    Chunk mid_chunk { text };
    Chunk right_chunk { right_string };

    auto iterator = m_chunks.begin() + chunk_index;
    m_chunks.erase(iterator);
    iterator = m_chunks.begin() + chunk_index;

    // Note reverse insertion order!
    iterator = m_chunks.insert(iterator, right_chunk);
    iterator = m_chunks.insert(iterator, mid_chunk);
    iterator = m_chunks.insert(iterator, left_chunk);

    fflush(f);
}

std::tuple<size_t, size_t> Line::chunk_index_for_position(size_t position)
{
    ASSERT(position < length());
    size_t seen { 0 };
    for (size_t i = 0; i < m_chunks.size(); ++i) {
        if (position < seen + m_chunks[i].length())
            return std::make_tuple(i, position - seen);
        seen += m_chunks[i].length();
    }
    ASSERT(false);
    return std::make_tuple(0, 0);
}

void Line::coalesce()
{
    if (m_chunks.size() <= 1)
        return;

    auto contents = data();
    m_chunks.clear();
    m_chunks.push_back(Chunk{ contents });
}
