compile_json_gui(CondFormatting.json CondFormattingUI.h cond_fmt_ui_json)
compile_json_gui(CondView.json CondFormattingViewUI.h cond_fmt_view_ui_json)

set(SOURCES
    Cell.cpp
    CellSyntaxHighlighter.cpp
    CellType/Date.cpp
    CellType/Format.cpp
    CellType/Identity.cpp
    CellType/Numeric.cpp
    CellType/String.cpp
    CellType/Type.cpp
    CellTypeDialog.cpp
    CondFormattingUI.h
    CondFormattingViewUI.h
    HelpWindow.cpp
    JSIntegration.cpp
    Spreadsheet.cpp
    SpreadsheetModel.cpp
    SpreadsheetView.cpp
    SpreadsheetWidget.cpp
    Workbook.cpp
    main.cpp
)

serenity_bin(Spreadsheet)
target_link_libraries(Spreadsheet LibGUI LibJS LibWeb)
