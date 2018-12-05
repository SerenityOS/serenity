#pragma once

#include "cuki.h"
#include <deque>
#include <string>
#include <tuple>

class Chunk {
public:
    explicit Chunk(const std::string&);
    ~Chunk();

    const std::string& data() const { return m_data; }
    size_t length() const { return m_data.size(); }

private:
    std::string m_data;
};

class Line {
    AK_MAKE_NONCOPYABLE(Line);
public:
    Line() { }
    Line(const std::string&);
    Line(Line&&);
    ~Line();

    std::string data() const;
    size_t length() const { return data().size(); }

    void insert(size_t index, const std::string&);

    std::string truncate(size_t length);

    void coalesce();

private:
    void append(const std::string&);
    void prepend(const std::string&);

    std::tuple<size_t, size_t> chunk_index_for_position(size_t);

    std::deque<Chunk> m_chunks;
};
