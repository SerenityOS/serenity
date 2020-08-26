set(SOURCES
    CellSyntaxHighlighter.cpp
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
