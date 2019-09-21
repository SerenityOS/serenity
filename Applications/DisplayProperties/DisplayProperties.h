#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CConfigFile.h>
#include <LibDraw/Color.h>
#include <LibDraw/Size.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GLabel.h>

class DisplayPropertiesWidget final {
public:
    enum class ButtonOperations {
        Ok,
        Apply,
        Cancel,
    };

    enum TabIndices {
        Wallpaper,
        Settings
    };

public:
    DisplayPropertiesWidget();

    // Apply the settings to the Window Server
    void send_settings_to_window_server(int tabIndex);
    void create_frame();

    const GWidget* root_widget() const { return m_root_widget; }
    GWidget* root_widget() { return m_root_widget; }

private:
    void create_wallpaper_list();
    void create_resolution_list();
    void create_root_widget();

private:
    String m_wallpaper_path;
    RefPtr<CConfigFile> m_wm_config;
    RefPtr<GWidget> m_root_widget;
    Vector<Size> m_resolutions;
    Vector<String> m_wallpapers;
    RefPtr<GLabel> m_wallpaper_preview;

    Size m_selected_resolution;
    String m_selected_wallpaper;
};
