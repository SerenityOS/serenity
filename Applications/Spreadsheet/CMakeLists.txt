compile_gml(CondFormatting.gml CondFormattingGML.h cond_fmt_gml)
compile_gml(CondView.gml CondFormattingViewGML.h cond_fmt_view_gml)

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
    CondFormattingGML.h
    CondFormattingViewGML.h
    HelpWindow.cpp
    JSIntegration.cpp
    Readers/XSV.cpp
    Spreadsheet.cpp
    SpreadsheetModel.cpp
    SpreadsheetView.cpp
    SpreadsheetWidget.cpp
    Workbook.cpp
    main.cpp
)

serenity_bin(Spreadsheet)
target_link_libraries(Spreadsheet LibGUI LibJS LibWeb)
