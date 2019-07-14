#include <AK/FileSystemPath.h>
#include <AK/Optional.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GDirectoryModel;
class GLabel;

class GFilePicker final : public GDialog {
public:
    enum class Mode {
        Open,
        Save
    };

    static Optional<String> get_open_filepath();
    static Optional<String> get_save_filepath();
    static bool file_exists(const StringView& path);

    GFilePicker(Mode type = Mode::Open, const StringView& path = "/", CObject* parent = nullptr);
    virtual ~GFilePicker() override;

    FileSystemPath selected_file() const { return m_selected_file; }

    virtual const char* class_name() const override { return "GFilePicker"; }

private:
    void set_preview(const FileSystemPath&);
    void clear_preview();

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

    GLabel* m_preview_image_label { nullptr };
    GLabel* m_preview_name_label { nullptr };
    GLabel* m_preview_geometry_label { nullptr };
    Mode m_mode { Mode::Open };
};