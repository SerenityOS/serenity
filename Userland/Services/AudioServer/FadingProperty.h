/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AudioServer {

// This is in buffer counts.
// As each buffer is approx 1/40 of a second, this means about 1/4 of a second of fade time.
constexpr int DEFAULT_FADE_TIME = 10;

// A property of an audio system that needs to fade briefly whenever changed.
template<typename T>
class FadingProperty {
public:
    FadingProperty(T const value)
        : FadingProperty(value, DEFAULT_FADE_TIME)
    {
    }
    FadingProperty(T const value, int const fade_time)
        : m_old_value(value)
        , m_new_value(move(value))
        , m_fade_time(fade_time)
    {
    }
    virtual ~FadingProperty()
    {
        m_old_value.~T();
        m_new_value.~T();
    }

    FadingProperty<T>& operator=(T const& new_value)
    {
        // The origin of the fade is wherever we're right now.
        m_old_value = static_cast<T>(*this);
        m_new_value = new_value;
        m_current_fade = 0;
        return *this;
    }
    FadingProperty<T>& operator=(FadingProperty<T> const&) = delete;

    operator T() const
    {
        if (!is_fading())
            return m_new_value;
        return m_old_value * (1 - m_current_fade) + m_new_value * (m_current_fade);
    }

    void advance_time()
    {
        m_current_fade += 1.0 / static_cast<double>(m_fade_time);
        m_current_fade = clamp(m_current_fade, 0.0, 1.0);
    }

    bool is_fading() const
    {
        return m_current_fade < 1;
    }

    T target() const { return m_new_value; }

private:
    T m_old_value {};
    T m_new_value {};
    double m_current_fade { 0 };
    int const m_fade_time;
};

}
