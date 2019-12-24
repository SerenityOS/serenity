#pragma once

#include <AK/Types.h>
#include <LibC/SharedBuffer.h>
#include <LibDraw/Color.h>

struct SystemTheme {
    Color desktop_background;

    Color active_window_border1;
    Color active_window_border2;
    Color active_window_title;

    Color inactive_window_border1;
    Color inactive_window_border2;
    Color inactive_window_title;

    Color moving_window_border1;
    Color moving_window_border2;
    Color moving_window_title;

    Color highlight_window_border1;
    Color highlight_window_border2;
    Color highlight_window_title;

    Color menu_stripe;
    Color menu_base;
    Color menu_selection;

    Color window;
    Color window_text;
    Color base;
    Color button;
    Color button_text;

    Color threed_highlight;
    Color threed_shadow1;
    Color threed_shadow2;

    Color hover_highlight;

    Color selection;
    Color selection_text;
};

const SystemTheme& current_system_theme();
int current_system_theme_buffer_id();
void set_system_theme(SharedBuffer&);
RefPtr<SharedBuffer> load_system_theme(const String& path);
