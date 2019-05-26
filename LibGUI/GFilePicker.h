#include <AK/FileSystemPath.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GDirectoryModel;
class GLabel;

class GFilePicker final : public GDialog {
public:
    GFilePicker(const String& path = "/", CObject* parent = nullptr);
    virtual ~GFilePicker() override;

    FileSystemPath selected_file() const { return m_selected_file; }

    virtual const char* class_name() const override { return "GFilePicker"; }

private:
    void set_preview(const FileSystemPath&);
    void clear_preview();

    GTableView* m_view { nullptr };
    Retained<GDirectoryModel> m_model;
    FileSystemPath m_selected_file;

    GLabel* m_preview_image_label { nullptr };
    GLabel* m_preview_name_label { nullptr };
    GLabel* m_preview_geometry_label { nullptr };
};
