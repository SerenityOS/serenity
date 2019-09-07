#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CConfigFile.h>
#include <LibDraw/Color.h>
#include <LibDraw/Size.h>
#include <LibGUI/GWidget.h>

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

    inline GWidget* get_root_widget() const { return m_root_widget; }

private:
    void create_wallpaper_list();
    void create_resolution_list();
    void create_root_widget();

private:
    String m_wallpaper_path;
    RefPtr<CConfigFile> m_wm_config;
    GWidget* m_root_widget { nullptr };
    Vector<Size> m_resolutions;
    Vector<String> m_wallpapers;

    Size m_selected_resolution;
    String m_selected_wallpaper;
};
