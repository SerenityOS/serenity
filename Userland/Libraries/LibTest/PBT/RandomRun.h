/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/PBT/Chunk.h>

#include <AK/QuickSort.h>
#include <AK/Vector.h>

#define MAX_RANDOMRUN_LENGTH (64UL * 1024UL) // 64k unsigned ints worth of random bits

/* RandomRun is a record of random bits used in generation of random values.
Once a value failing user test is found, we then attempt to shrink its RandomRun
using various ShrinkCmds.

This means that we construct new RandomRuns by saying "OK, but what if the PRNG
gave you 0 instead of 23 that time..." The runner then tries to generate a new
value from the new RandomRun; if it succeeds and the value still fails the test,
we've shrunk our counterexample some!

RandomRun is conceptually a sequence of unsigned integers, e.g. [5,3,10,8,0,0,1].
*/
class RandomRun {
public:
    RandomRun() { m_data = Vector<u32>(); }
    RandomRun(RandomRun&& rhs) { m_data = rhs.m_data; }
    RandomRun(RandomRun const& rhs) { m_data = rhs.m_data; }
    RandomRun(Vector<u32> const& rhs) { m_data = rhs; }
    RandomRun& operator=(RandomRun const& rhs)
    {
        if (this != &rhs) {
            m_data = rhs.m_data;
            m_current_index = rhs.m_current_index;
        }
        return *this;
    }
    bool is_empty() const { return m_data.is_empty(); }
    bool is_full() { return m_data.size() >= MAX_RANDOMRUN_LENGTH; }
    bool has_a_chance(Chunk const& c) const
    {
        // Is the chunk fully inside the RandomRun?
        // Example:
        // size: 6
        // 0 1 2 3 4 5
        //     ^ ^ ^ ^
        // chunk size 4
        //       index 2
        return (c.index + c.size <= m_data.size());
    }
    void append(u32 n) { m_data.append(n); }
    size_t size() const { return m_data.size(); }
    Optional<u32> next()
    {
        if (m_current_index < m_data.size()) {
            return m_data[m_current_index++];
        }
        return Optional<u32> {};
    }
    u32& operator[](size_t index) { return m_data[index]; }
    u32 operator[](size_t index) const { return m_data[index]; }
    u32 at(size_t index) const { return m_data[index]; }
    bool operator==(RandomRun const& rhs) const { return m_data == rhs.m_data; }
    bool operator!=(RandomRun const& rhs) const { return m_data != rhs.m_data; }
    bool operator<(RandomRun const& rhs) const
    {
        if (size() < rhs.size())
            return true;

        if (size() > rhs.size())
            return false;

        for (size_t i = 0; i < size(); i++) {
            if (at(i) < rhs.at(i))
                return true;

            if (at(i) > rhs.at(i))
                return false;
        }
        return false;
    }
    bool operator<=(RandomRun const& rhs) const
    {
        return (*this < rhs) || (*this == rhs);
    }
    RandomRun with_sorted(Chunk c) const
    {
        Vector<u32> new_data = m_data;
        AK::dual_pivot_quick_sort(
            new_data,
            c.index,
            c.index + c.size - 1,
            [](auto& a, auto& b) { return a < b; });
        return RandomRun(new_data);
    }
    RandomRun with_deleted(Chunk c) const
    {
        Vector<u32> new_data(m_data);
        new_data.remove(c.index, c.size);
        return RandomRun(new_data);
    }
    ErrorOr<String> to_string()
    {
        return String::formatted("[{}]", TRY(String::join(',', m_data, "{}"sv)));
    }

private:
    Vector<u32> m_data;
    size_t m_current_index = 0;
};

template<>
struct AK::Formatter<RandomRun> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, RandomRun run)
    {
        return Formatter<StringView>::format(builder, TRY(run.to_string()));
    }
};
