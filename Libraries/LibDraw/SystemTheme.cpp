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

#define DO_COLOR(x) \
    data->color[(int)ColorRole::x] = get(#x)

    DO_COLOR(DesktopBackground);
    DO_COLOR(ThreedHighlight);
    DO_COLOR(ThreedShadow1);
    DO_COLOR(ThreedShadow2);
    DO_COLOR(HoverHighlight);
    DO_COLOR(Selection);
    DO_COLOR(SelectionText);
    DO_COLOR(Window);
    DO_COLOR(WindowText);
    DO_COLOR(Base);
    DO_COLOR(BaseText);
    DO_COLOR(Button);
    DO_COLOR(ButtonText);
    DO_COLOR(DesktopBackground);
    DO_COLOR(ActiveWindowBorder1);
    DO_COLOR(ActiveWindowBorder2);
    DO_COLOR(ActiveWindowTitle);
    DO_COLOR(InactiveWindowBorder1);
    DO_COLOR(InactiveWindowBorder2);
    DO_COLOR(InactiveWindowTitle);
    DO_COLOR(MovingWindowBorder1);
    DO_COLOR(MovingWindowBorder2);
    DO_COLOR(MovingWindowTitle);
    DO_COLOR(HighlightWindowBorder1);
    DO_COLOR(HighlightWindowBorder2);
    DO_COLOR(HighlightWindowTitle);
    DO_COLOR(MenuStripe);
    DO_COLOR(MenuBase);
    DO_COLOR(MenuSelection);

    buffer->seal();
    buffer->share_globally();

    return buffer;
}
