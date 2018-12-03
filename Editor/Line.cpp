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

    auto chunkAddress = chunkIndexForPosition(index);
    auto chunkIndex = std::get<0>(chunkAddress);
    auto& chunk = m_chunks[chunkIndex];
    auto indexInChunk = std::get<1>(chunkAddress);

    static FILE* f = fopen("log", "a");
    fprintf(f, "#Column:%zu, Chunk:%zu, Index:%zu\n", index, chunkIndex, indexInChunk);
    
    auto leftString = chunk.data().substr(0, indexInChunk);
    auto rightString = chunk.data().substr(indexInChunk, chunk.length() - indexInChunk);

    fprintf(f, "#{\"%s\", \"%s\", \"%s\"}\n", leftString.c_str(), text.c_str(), rightString.c_str());
    fflush(f);

    Chunk leftChunk { leftString };
    Chunk midChunk { text };
    Chunk rightChunk { rightString };

    auto iterator = m_chunks.begin() + chunkIndex;
    m_chunks.erase(iterator);
    iterator = m_chunks.begin() + chunkIndex;

    // Note reverse insertion order!
    m_chunks.insert(iterator, rightChunk);
    m_chunks.insert(iterator, midChunk);
    m_chunks.insert(iterator, leftChunk);
}

std::tuple<size_t, size_t> Line::chunkIndexForPosition(size_t position)
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
