#include <LibGUI/GTreeView.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

struct Node {
    String text;
    Node* parent { nullptr };
    Vector<Node*> children;
};

class TestModel : public GModel {
public:
    static Retained<TestModel> create() { return adopt(*new TestModel); }

    TestModel();

    virtual int row_count(const GModelIndex& = GModelIndex()) const override;
    virtual int column_count(const GModelIndex& = GModelIndex()) const override;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const override;
    virtual void update() override;
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& parent = GModelIndex()) const override;
    virtual ColumnMetadata column_metadata(int) const override{ return { 100 }; }

    Node* m_root { nullptr };
};

Node* make_little_tree(int depth, Node* parent)
{
    static int next_id = 0;
    Node* node = new Node;
    node->text = String::format("Node #%d", next_id++);
    node->parent = parent;
    if (depth)
        node->children.append(make_little_tree(depth - 1, node));
    return node;
}

GModelIndex TestModel::index(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid())
        return create_index(row, column, m_root);
    auto& node = *(Node*)parent.internal_data();
    return create_index(row, column, node.children[row]);
}

TestModel::TestModel()
{
    m_root = new Node;
    m_root->text = "Root";

    m_root->children.append(make_little_tree(3, m_root));
    m_root->children.append(make_little_tree(2, m_root));
    m_root->children.append(make_little_tree(1, m_root));
}

int TestModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return 1;
    auto& node = *(const Node*)index.internal_data();
    return node.children.size();
}

int TestModel::column_count(const GModelIndex&) const
{
    return 1;
}

void TestModel::update()
{
}

GVariant TestModel::data(const GModelIndex& index, Role role) const
{
    if (!index.is_valid())
        return { };
    auto& node = *(const Node*)index.internal_data();
    if (role == GModel::Role::Display) {
        return node.text;
    }
    if (role == GModel::Role::Icon) {
        if (node.children.is_empty())
            return GIcon::default_icon("filetype-unknown");
        return GIcon::default_icon("filetype-folder");
    }
    return { };
}

struct GTreeView::MetadataForIndex {
    bool open { false };
};


GTreeView::MetadataForIndex& GTreeView::ensure_metadata_for_index(const GModelIndex& index) const
{
    ASSERT(index.is_valid());
    auto it = m_view_metadata.find(index.internal_data());
    if (it != m_view_metadata.end())
        return *it->value;
    auto new_metadata = make<MetadataForIndex>();
    auto& new_metadata_ref = *new_metadata;
    m_view_metadata.set(index.internal_data(), move(new_metadata));
    return new_metadata_ref;
}

GTreeView::GTreeView(GWidget* parent)
    : GAbstractView(parent)
{
    set_frame_shape(GFrame::Shape::Container);
    set_frame_shadow(GFrame::Shadow::Sunken);
    set_frame_thickness(2);

    set_model(TestModel::create());
}

GTreeView::~GTreeView()
{
}

GModelIndex GTreeView::index_at_content_position(const Point& position) const
{
    if (!model())
        return { };
    GModelIndex result;
    traverse_in_paint_order([&] (const GModelIndex& index, const Rect& rect) {
        if (rect.contains(position)) {
            result = index;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    return result;
}

void GTreeView::mousedown_event(GMouseEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    auto adjusted_position = event.position().translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
    auto index = index_at_content_position(adjusted_position);
    if (!index.is_valid()) {
        dbgprintf("GTV::mousedown: No valid index at %s (adjusted to: %s)\n", event.position().to_string().characters(), adjusted_position.to_string().characters());
        return;
    }
    dbgprintf("GTV::mousedown: Index %d,%d {%p}] at %s (adjusted to: %s)\n", index.row(), index.column(), index.internal_data(), event.position().to_string().characters(), adjusted_position.to_string().characters());
    auto& metadata = ensure_metadata_for_index(index);

    if (model.row_count(index)) {
        metadata.open = !metadata.open;
        dbgprintf("GTV::mousedown: toggle index %d,%d {%p} open: %d -> %d\n", index.row(), index.column(), index.internal_data(), !metadata.open, metadata.open);
        update();
    }
}

template<typename Callback>
void GTreeView::traverse_in_paint_order(Callback callback) const
{
    ASSERT(model());
    auto& model = *this->model();
    int indent_level = 0;
    int y_offset = 0;
    auto visible_content_rect = this->visible_content_rect();

    Function<IterationDecision(const GModelIndex&)> traverse_index = [&] (const GModelIndex& index) {
        if (index.is_valid()) {
            auto& metadata = ensure_metadata_for_index(index);
            int x_offset = indent_level * indent_width_in_pixels();
            auto node_text = model.data(index, GModel::Role::Display).to_string();
            Rect rect = {
                x_offset, y_offset,
                icon_size() + icon_spacing() + font().width(node_text), item_height()
            };
            if (rect.intersects(visible_content_rect)) {
                if (callback(index, rect) == IterationDecision::Abort)
                    return IterationDecision::Abort;
            }
            y_offset += item_height();
            // NOTE: Skip traversing children if this index is closed!
            if (!metadata.open)
                return IterationDecision::Continue;
        }

        ++indent_level;
        for (int i = 0; i < model.row_count(index); ++i) {
            if (traverse_index(model.index(i, 0, index)) == IterationDecision::Abort)
                return IterationDecision::Abort;
        }
        --indent_level;
        return IterationDecision::Continue;
    };
    traverse_index(model.index(0, 0, GModelIndex()));
}

void GTreeView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);
    painter.translate(frame_inner_rect().location());

    if (!model())
        return;
    auto& model = *this->model();

    traverse_in_paint_order([&] (const GModelIndex& index, const Rect& rect) {
        painter.fill_rect(rect, Color::LightGray);
        Rect icon_rect = { rect.x(), rect.y(), icon_size(), icon_size() };
        auto icon = model.data(index, GModel::Role::Icon);
        if (icon.is_icon()) {
            if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size()))
                painter.blit(rect.location(), *bitmap, bitmap->rect());
        }
        Rect text_rect = {
            icon_rect.right() + 1 + icon_spacing(), rect.y(),
            rect.width() - icon_size() - icon_spacing(), rect.height()
        };
        auto node_text = model.data(index, GModel::Role::Display).to_string();
        painter.draw_text(text_rect, node_text, TextAlignment::CenterLeft, Color::Black);
        return IterationDecision::Continue;
    });
}
