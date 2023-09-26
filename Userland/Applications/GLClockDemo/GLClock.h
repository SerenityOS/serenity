/*
 * Copyright (c) 2023, Glenford Williams <hey@glenfordwilliams.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Time.h>
#include <LibCore/DateTime.h>
#include <LibGUI/OpenGLWidget.h>
#include <LibTimeZone/TimeZone.h>
#include <LibTimeZone/TimeZoneData.h>

namespace GLClockDemo {

class GLClock : public GUI::OpenGLWidget {
    C_OBJECT(GLClock);

private:
    String m_timezone;
    Core::DateTime m_time;

    void set_time()
    {
        if (timezone().is_empty()) {
            auto converted = String::from_deprecated_string(TimeZone::current_time_zone().to_deprecated_string());
            set_timezone(converted.release_value());
        }

        auto t = TimeZone::time_zone_from_string(timezone());

        if (!t.has_value()) {
            VERIFY_NOT_REACHED();
        }

        auto offset = TimeZone::get_time_zone_offset(t.release_value(), UnixDateTime::now());

        if (!offset.has_value())
            VERIFY_NOT_REACHED();

        auto timestamp = UnixDateTime::now().seconds_since_epoch() + offset->seconds;
        struct tm tm;

        gmtime_r(&timestamp, &tm);

        auto time = mktime(&tm);
        m_time = Core::DateTime::from_timestamp(time);
    }

    static void draw_circle(float radius, int segments, Color color)
    {
        auto c = Gfx::Color(color);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(c.red(), c.green(), c.blue(), c.alpha());
        glBegin(GL_TRIANGLE_FAN);

        // Center vertex of the fan
        glVertex2f(0.0f, 0.0f);

        // Loop to create the fan
        for (int i = 0; i <= segments; ++i) {
            float theta = 2.0 * M_PI * i / segments;
            float x = radius * cosf(theta);
            float y = radius * sinf(theta);
            glVertex2f(x, y);
        }

        glEnd();
        glDisable(GL_BLEND);
    }

    static void draw_line(float x1, float y1, float x2, float y2, float thickness, Color const& color)
    {
        glEnable(GL_POLYGON_SMOOTH);
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float angle = atan2f(y2 - y1, x2 - x1);
        float halfThick = thickness / 2.0f;

        float xOff = halfThick * sinf(angle);
        float yOff = halfThick * cosf(angle);

        auto c = Gfx::Color(color);
        glColor4f(c.red(), c.green(), c.blue(), c.alpha());

        glBegin(GL_QUADS);

        glVertex2f(x1 - xOff, y1 + yOff);
        glVertex2f(x2 - xOff, y2 + yOff);
        glVertex2f(x2 + xOff, y2 - yOff);
        glVertex2f(x1 + xOff, y1 - yOff);

        glEnd();
        glDisable(GL_POLYGON_SMOOTH);
        glDisable(GL_BLEND);
    }

    static void draw_hand(float length, float angle, Gfx::Color const& color)
    {
        float x1 = 0.0f;
        float y1 = 0.0f;
        float x2 = length * cosf(angle);
        float y2 = length * sinf(angle);

        draw_line(x1, y1, x2, y2, 0.05f, color);
    }

    static void hour_markings()
    {
        for (int i = 0; i < 12; ++i) {
            float angle = (2.0 * M_PI / 12) * i;
            float innerRadius = 0.9f; // 90% of the clock's radius
            float outerRadius = 1.0f;

            float x1 = innerRadius * cos(angle);
            float y1 = innerRadius * sin(angle);
            float x2 = outerRadius * cos(angle);
            float y2 = outerRadius * sin(angle);

            draw_line(x1, y1, x2, y2, 0.025f, Color::Red);
        }
    }

    static void minute_markings()
    {
        for (int i = 0; i < 60; ++i) {
            if (i % 5 != 0) {
                float angle = (2.0 * M_PI / 60) * i;
                float innerRadius = 0.95f; // 95% of the clock's radius
                float outerRadius = 1.0f;

                float x1 = innerRadius * cos(angle);
                float y1 = innerRadius * sin(angle);
                float x2 = outerRadius * cos(angle);
                float y2 = outerRadius * sin(angle);

                draw_line(x1, y1, x2, y2, 0.01f, Color(0, 0, 200));
            }
        }
    }

    virtual void timer_event(Core::TimerEvent&) override
    {
        set_time();
        update();
    }

public:
    String timezone() const { return m_timezone; }

    void set_timezone(String tooltip)
    {
        m_timezone = move(tooltip);
    }

    GLClock()
    {
        start_timer(500);
    }

    virtual ~GLClock() override = default;

protected:
    void initialize_gl() override
    {
        set_time();
        glClearColor(0, 0, 0, 0);
    }

    void paint_gl() override
    {
        glClear(GL_COLOR_BUFFER_BIT);

        glPushMatrix();
        glRotatef(90, 0.0, 0.0, 1.0);

        draw_circle(1.0f, 100, Color::White);

        hour_markings();
        minute_markings();

        auto seconds = m_time.second();
        auto minutes = m_time.minute();
        auto hours = m_time.hour() % 12;

        float secondsAngle = -(2.0 * M_PI / 60) * seconds;

        float minuteAngle = -(2.0 * M_PI / 60) * minutes;

        float hourAngle = -(2.0 * M_PI / 12) * (hours + static_cast<double>(minutes) / 60.0);

        draw_hand(0.7f, hourAngle, Color::Blue);
        draw_hand(0.8f, minuteAngle, Color::Green);
        draw_hand(0.8f, secondsAngle, Color::Red);
        draw_circle(0.05f, 50, Color::Black);
        glPopMatrix();
    }

    void resize_gl(int w, int h) override
    {
        glViewport(0, 0, w, h);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        double aspect_ratio = static_cast<double>(w) / static_cast<double>(h);
        if (aspect_ratio > 1.0) {
            glOrtho(-2.0 * aspect_ratio, 2.0 * aspect_ratio, -2.0, 2.0, -1, 1);
        } else {
            glOrtho(-2.0, 2.0, -2.0 / aspect_ratio, 2.0 / aspect_ratio, -1, 1);
        }

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        if (aspect_ratio > 1.0) {
            glScalef(1.0 / aspect_ratio, 1.0, 1.0); // Scale down X dimension
        } else {
            glScalef(1.0, aspect_ratio, 1.0); // Scale down Y dimension
        }
    }
};
};
