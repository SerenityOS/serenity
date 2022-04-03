/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

class Square {
public:
    int get_value() { return m_value; };
    void set_value(int value);
    void set_fixed(bool is_fixed) { m_fixed = is_fixed; };
    bool is_fixed() { return m_fixed; };
    void set_answer(int value);
    bool is_correct();

private:
    bool m_fixed { false };
    int m_value { 0 };
    int m_answer;
};
