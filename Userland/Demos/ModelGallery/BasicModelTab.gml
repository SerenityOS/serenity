@GUI::Widget {
    name: "basic_model_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Label {
        text: "Here is a basic model, displayed on a table widget. Its clients are updated via granular updates. You can add or remove items with the widgets below."
        text_alignment: "CenterLeft"
        fixed_height: 34
    }

    @GUI::TableView {
        name: "model_table"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 30

        @GUI::TextBox {
            name: "new_item_name"
            placeholder: "Enter some text to be added..."
        }

        @GUI::Button {
            name: "add_new_item"
            fixed_width: 22
            fixed_height: 22
            tooltip: "Add the text as an item to the model"
        }

        @GUI::Button {
            name: "remove_selected_item"
            fixed_width: 22
            fixed_height: 22
            tooltip: "Remove the selected item from the model"
        }
    }
}
