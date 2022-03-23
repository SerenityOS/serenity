/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 * Copyright (c) 2022, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DigitalClock.h"
#include "AK/StringView.h"
#include "LibGUI/Painter.h"
#include "LibGfx/AntiAliasingPainter.h"
#include "LibGfx/Color.h"
#include "LibGfx/Forward.h"
#include "math.h"
#include <LibCore/DateTime.h>
#include <LibGUI/Application.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>

void DigitalClock::draw_digit(Gfx::AntiAliasingPainter& painter, int num, Gfx::IntPoint pos, int digitWidth, int padding)
{
    // Create the size of the digit, (Make height double the width)
    Gfx::IntPoint size = Gfx::IntPoint({ digitWidth, digitWidth * 2 });

    // Get the x and y coordinates of the top left corner of the digit
    float x = pos.x() - size.x() / 2;
    float y = pos.y() - size.y() / 2;

    // Split the x and y coordinates so we are able to use them to position the segments
    // 	 E.G. {y + (yPos * 0), x + (xPos * 1)} will give us the bottom left corner of the segment
    //	 and  {y + (yPos * 1), x + (xPos * 0)} will give us the top right corner of the segment
    float xPos = size.x() / 2;
    float yPos = size.y() / 2;

    // Generate the overall size of the digits segments
    float segmentShortSize = size.x() / 6;
    float segmentLongSize = size.y() / 2;

    // Top segment
    draw_edge_segment(painter, { x + (xPos * 0), y + (yPos * 0) }, segmentLongSize, segmentShortSize, 1, padding, (num == 0 || num == 2 || num == 3 || num == 5 || num == 6 || num == 7 || num == 8 || num == 9) ? light : dark);

    // Top left segment
    draw_side_segment(painter, { x + (xPos * 0), y + (yPos * 0) }, segmentShortSize, segmentLongSize, 1, -1, padding, (num == 0 || num == 4 || num == 5 || num == 6 || num == 8 || num == 9) ? light : dark);

    // Top right segment
    draw_side_segment(painter, { x + (xPos * 2), y + (yPos * 0) }, segmentShortSize, segmentLongSize, -1, -1, padding, (num == 0 || num == 1 || num == 2 || num == 3 || num == 4 || num == 7 || num == 8 || num == 9) ? light : dark);
  
    // Middle segment
    draw_middle_segment(painter, { x + (xPos * 0), y + (yPos * 1) }, segmentLongSize, segmentShortSize, padding, (num == 2 || num == 3 || num == 4 || num == 5 || num == 6 || num == 8 || num == 9) ? light : dark);

	// Bottom segment
    draw_edge_segment(painter, { x + (xPos * 0), y + (yPos * 2) }, segmentLongSize, segmentShortSize, -1, padding, (num == 0 || num == 2 || num == 3 || num == 5 || num == 6 || num == 8 || num == 9) ? light : dark);

    // Bottom left segment
    draw_side_segment(painter, { x + (xPos * 0), y + (yPos * 1) }, segmentShortSize, segmentLongSize, 1, 1, padding, (num == 0 || num == 2 || num == 6 || num == 8) ? light : dark);

    // Bottom Right segment
    draw_side_segment(painter, { x + (xPos * 2), y + (yPos * 1) }, segmentShortSize, segmentLongSize, -1, 1, padding, (num == 0 || num == 1 || num == 3 || num == 4 || num == 5 || num == 6 || num == 7 || num == 8 || num == 9) ? light : dark);


};

void DigitalClock::draw_middle_segment(Gfx::AntiAliasingPainter& painter, Gfx::IntPoint pos, int width, int height, int padding, Gfx::Color segmentColor)
{
    // Get the x and y coordinates, with padding added
    int x = pos.x() + (padding / 2);
    int y = pos.y();

	// Create the path for the segment
    Gfx::Path segmentPath;

    segmentPath.move_to({ x, y });
    segmentPath.line_to({ x + height, y + (height / 2) });
    segmentPath.line_to({ x + (width - padding) - height, y + (height / 2) });
    segmentPath.line_to({ x + (width - padding), y });
    segmentPath.line_to({ x + (width - padding) - height, y - (height / 2) });
    segmentPath.line_to({ x + height, y - (height / 2) });
    segmentPath.close();

	// Draw the segment with the specified color
    painter.fill_path(segmentPath, segmentColor, Gfx::Painter::WindingRule::EvenOdd);
}

void DigitalClock::draw_edge_segment(Gfx::AntiAliasingPainter& painter, Gfx::IntPoint pos, int width, int height, int direction, int padding, Gfx::Color segmentColor)
{
	// Get the x and y coordinates, with padding added
    int x = pos.x() + (padding / 2);
    int y = pos.y();

	// Create the path for the segment
    Gfx::Path segmentPath;

    segmentPath.move_to({ x, y });
    segmentPath.line_to({ x + (width - padding), y });
    segmentPath.line_to({ x + (width - padding) - height, y + (height * direction) });
    segmentPath.line_to({ x + height, y + (height * direction) });
    segmentPath.close();

	// Draw the segment with the specified color
    painter.fill_path(segmentPath, segmentColor, Gfx::Painter::WindingRule::EvenOdd);
}

void DigitalClock::draw_side_segment(Gfx::AntiAliasingPainter& painter, Gfx::IntPoint pos, int width, int height, int sideDir, int upDir, int padding, Gfx::Color segmentColor)
{
	// Get the x and y coordinates, with padding added
    int x = pos.x();
    int y = pos.y() + (padding / 2);

	// Create the path for the segment
    Gfx::Path segmentPath;

    segmentPath.move_to({ x, y });
    if (upDir == -1) {
        segmentPath.line_to({ x + (width * sideDir), y + ((width * sideDir) * sideDir) });
        segmentPath.line_to({ x + (width * sideDir), y + (height - padding) - (((width * sideDir) / 2) * sideDir) });
    } else if (upDir == 1) {
        segmentPath.line_to({ x + (width * sideDir), y + (((width * sideDir) / 2) * sideDir) });
        segmentPath.line_to({ x + (width * sideDir), y + (height - padding) - ((width * sideDir) * sideDir) });
    }
    segmentPath.line_to({ x, y + (height - padding) });
    segmentPath.close();

	// Draw the segment with the specified color
    painter.fill_path(segmentPath, segmentColor, Gfx::Painter::WindingRule::EvenOdd);
}

void DigitalClock::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    Gfx::AntiAliasingPainter aa_painter(painter);

    painter.clear_rect(event.rect(), m_show_window_frame ? Gfx::Color::Black : Gfx::Color::Transparent);

    auto time = Core::DateTime::now();

    float paddingPercentage = 0.90;

    int columnWidth = int(rect().width() / 9);
    int columnYPos = int(rect().height() / 2);
    int digitSize = int(columnWidth * paddingPercentage);
    int padding = columnWidth * (1 - paddingPercentage);

	// Hours
    draw_digit(aa_painter, int(time.hour() / 10), { columnWidth * 1, columnYPos }, digitSize, padding);
    draw_digit(aa_painter, int(time.hour() % 10), { columnWidth * 2, columnYPos }, digitSize, padding);
    draw_colon(aa_painter, { columnWidth * 3, columnYPos }, digitSize, (time.second() % 2 == 0 ? light : dark));

	// Minutes
    draw_digit(aa_painter, int(time.minute() / 10), { columnWidth * 4, columnYPos }, digitSize, padding);
    draw_digit(aa_painter, int(time.minute() % 10), { columnWidth * 5, columnYPos }, digitSize, padding);
    draw_colon(aa_painter, { columnWidth * 6, columnYPos }, digitSize, (time.second() % 2 == 0 ? light : dark));

	// Seconds
    draw_digit(aa_painter, int(time.second() / 10), { columnWidth * 7, columnYPos }, digitSize, padding);
    draw_digit(aa_painter, int(time.second() % 10), { columnWidth * 8, columnYPos }, digitSize, padding);

    if (time.hour() == 0)
        update_title_date();
}

void DigitalClock::draw_colon(Gfx::AntiAliasingPainter& painter, Gfx::IntPoint pos, int size, Gfx::Color segmentColor)
{
    // Generate the top colon
    Gfx::Path topColon;
    topColon.move_to({ pos.x() + size / 8, (pos.y() + size / 8) + size / 2 });
    topColon.elliptical_arc_to({ pos.x() + size / 8, (pos.y() + size / 8) + size / 2 }, { size / 8, size / 8 }, 0.0, true, false);
    topColon.close();

    // Generate the bottom colon
    Gfx::Path bottomColon;
    bottomColon.move_to({ pos.x() + size / 8, (pos.y() - size / 8) - size / 2 });
    bottomColon.elliptical_arc_to({ pos.x() + size / 8, (pos.y() - size / 8) - size / 2 }, { size / 8, size / 8 }, 0.0, true, false);
    bottomColon.close();

	// Draw the colon
    painter.fill_path(topColon, segmentColor, Gfx::Painter::WindingRule::EvenOdd);
    painter.fill_path(bottomColon, segmentColor, Gfx::Painter::WindingRule::EvenOdd);
}

void DigitalClock::update_title_date()
{
    window()->set_title(Core::DateTime::now().to_string("%Y-%m-%d"));
}

void DigitalClock::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}

void DigitalClock::set_show_window_frame(bool show)
{
    if (show == m_show_window_frame)
        return;
    m_show_window_frame = show;
    auto& w = *window();
    w.set_frameless(!m_show_window_frame);
    w.set_has_alpha_channel(!m_show_window_frame);
    w.set_alpha_hit_threshold(m_show_window_frame ? 0 : 1);
}
