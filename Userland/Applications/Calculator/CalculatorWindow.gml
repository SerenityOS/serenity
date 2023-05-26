@Calculator::CalculatorWidget {
    fixed_width: 250
    fixed_height: 215
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::HorizontalSeparator {
            fixed_height: 2
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [8]
            }

            @GUI::TextBox {
                name: "entry_textbox"
                font_fixed_width: true
                mode: "DisplayOnly"
                focus_policy: "NoFocus"
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Label {
                    name: "label"
                    fixed_width: 35
                    fixed_height: 27
                }

                @GUI::Widget {
                    fixed_width: 5
                }

                @GUI::Button {
                    name: "backspace_button"
                    text: "Backspace"
                    fixed_width: 65
                    fixed_height: 28
                    foreground_role: "SyntaxControlKeyword"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "clear_error_button"
                    text: "CE"
                    fixed_width: 56
                    fixed_height: 28
                    foreground_role: "SyntaxControlKeyword"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "clear_button"
                    text: "C"
                    fixed_width: 60
                    fixed_height: 28
                    foreground_role: "SyntaxControlKeyword"
                    focus_policy: "NoFocus"
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Button {
                    name: "mem_clear_button"
                    text: "MC"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxPreprocessorValue"
                    focus_policy: "NoFocus"
                }

                @GUI::Widget {
                    fixed_width: 5
                }

                @GUI::Button {
                    name: "7_button"
                    text: "7"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "8_button"
                    text: "8"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "9_button"
                    text: "9"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "divide_button"
                    text: "/"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxOperator"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "sqrt_button"
                    text: "sqrt"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxFunction"
                    focus_policy: "NoFocus"
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Button {
                    name: "mem_recall_button"
                    text: "MR"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxPreprocessorValue"
                    focus_policy: "NoFocus"
                }

                @GUI::Widget {
                    fixed_width: 5
                }

                @GUI::Button {
                    name: "4_button"
                    text: "4"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "5_button"
                    text: "5"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "6_button"
                    text: "6"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "multiply_button"
                    text: "*"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxOperator"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "mod_button"
                    text: "%"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxFunction"
                    focus_policy: "NoFocus"
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Button {
                    name: "mem_save_button"
                    text: "MS"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxPreprocessorValue"
                    focus_policy: "NoFocus"
                }

                @GUI::Widget {
                    fixed_width: 5
                }

                @GUI::Button {
                    name: "1_button"
                    text: "1"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "2_button"
                    text: "2"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "3_button"
                    text: "3"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "subtract_button"
                    text: "-"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxOperator"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "inverse_button"
                    text: "1/x"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxFunction"
                    focus_policy: "NoFocus"
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Button {
                    name: "mem_add_button"
                    text: "M+"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxPreprocessorValue"
                    focus_policy: "NoFocus"
                }

                @GUI::Widget {
                    fixed_width: 5
                }

                @GUI::Button {
                    name: "0_button"
                    text: "0"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "sign_button"
                    text: "+/-"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "decimal_button"
                    text: "."
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxNumber"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "add_button"
                    text: "+"
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxOperator"
                    focus_policy: "NoFocus"
                }

                @GUI::Button {
                    name: "equal_button"
                    text: "="
                    fixed_width: 35
                    fixed_height: 28
                    foreground_role: "SyntaxPreprocessorValue"
                    focus_policy: "NoFocus"
                }
            }
        }
    }
}
