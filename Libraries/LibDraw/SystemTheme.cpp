#include <LibCore/CConfigFile.h>
#include <LibDraw/SystemTheme.h>

static SystemTheme dummy_theme;
static const SystemTheme* theme_page = &dummy_theme;
static RefPtr<SharedBuffer> theme_buffer;

const SystemTheme& current_system_theme()
{
    ASSERT(theme_page);
    return *theme_page;
}

int current_system_theme_buffer_id()
{
    ASSERT(theme_buffer);
    return theme_buffer->shared_buffer_id();
}

void set_system_theme(SharedBuffer& buffer)
{
    theme_buffer = buffer;
    theme_page = (SystemTheme*)theme_buffer->data();
}

RefPtr<SharedBuffer> load_system_theme(const String& path)
{
    auto file = CConfigFile::open(path);
    auto buffer = SharedBuffer::create_with_size(sizeof(SystemTheme));

    dbg() << "Created shared buffer with id " << buffer->shared_buffer_id();

    auto* data = (SystemTheme*)buffer->data();

    auto get = [&](auto& name) {
        auto color_string = file->read_entry("Colors", name);
        auto color = Color::from_string(color_string);
        if (!color.has_value())
            return Color(Color::Black);
        dbg() << "Parsed system color '" << name << "' = " << color.value();
        return color.value();
    };

    data->desktop_background = get("DesktopBackground");
    data->threed_highlight = get("ThreedHighlight");
    data->threed_shadow1 = get("ThreedShadow1");
    data->threed_shadow2 = get("ThreedShadow2");
    data->hover_highlight = get("HoverHighlight");
    data->window = get("Window");
    data->window_text = get("WindowText");
    data->base = get("Base");
    data->button = get("Button");
    data->button_text = get("ButtonText");
    data->desktop_background = get("DesktopBackground");
    data->active_window_border1 = get("ActiveWindowBorder1");
    data->active_window_border2 = get("ActiveWindowBorder2");
    data->active_window_title = get("ActiveWindowTitle");
    data->inactive_window_border1 = get("InactiveWindowBorder1");
    data->inactive_window_border2 = get("InactiveWindowBorder2");
    data->inactive_window_title = get("InactiveWindowTitle");
    data->moving_window_border1 = get("MovingWindowBorder1");
    data->moving_window_border2 = get("MovingWindowBorder2");
    data->moving_window_title = get("MovingWindowTitle");
    data->highlight_window_border1 = get("HighlightWindowBorder1");
    data->highlight_window_border2 = get("HighlightWindowBorder2");
    data->highlight_window_title = get("HighlightWindowTitle");
    data->menu_stripe = get("MenuStripe");
    data->menu_base = get("MenuBase");
    data->menu_selection = get("MenuSelection");

    buffer->seal();
    buffer->share_globally();

    return buffer;
}
