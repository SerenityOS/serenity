set(SOURCES
    Cell.cpp
    CellSyntaxHighlighter.cpp
    CellType/Format.cpp
    CellType/Identity.cpp
    CellType/Numeric.cpp
    CellType/String.cpp
    CellType/Type.cpp
    CellTypeDialog.cpp
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
