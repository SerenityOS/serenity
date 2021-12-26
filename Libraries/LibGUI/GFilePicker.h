#include <AK/FileSystemPath.h>
#include <AK/Optional.h>
#include <LibCore/CUserInfo.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GDirectoryModel;
class GLabel;
class GTextBox;

class GFilePicker final : public GDialog {
    C_OBJECT(GFilePicker)
public:
    enum class Mode {
        Open,
        Save
    };

    static Optional<String> get_open_filepath();
    static Optional<String> get_save_filepath(const String& title, const String& extension);
    static bool file_exists(const StringView& path);

    GFilePicker(Mode type = Mode::Open, const StringView& file_name = "Untitled", const StringView& path = String(get_current_user_home_path()), CObject* parent = nullptr);
    virtual ~GFilePicker() override;

    FileSystemPath selected_file() const { return m_selected_file; }

private:
    void set_preview(const FileSystemPath&);
    void clear_preview();
    void on_file_return();

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
            return "Open";
        case Mode::Save:
            return "Save";
        default:
            return "OK";
        }
    }

    GTableView* m_view { nullptr };
    NonnullRefPtr<GDirectoryModel> m_model;
    FileSystemPath m_selected_file;

    ObjectPtr<GTextBox> m_filename_textbox;
    ObjectPtr<GLabel> m_preview_image_label;
    ObjectPtr<GLabel> m_preview_name_label;
    ObjectPtr<GLabel> m_preview_geometry_label;
    Mode m_mode { Mode::Open };
};
