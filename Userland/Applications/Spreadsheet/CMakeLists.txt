serenity_component(
    Spreadsheet
    TARGETS Spreadsheet
)

compile_gml(CondFormatting.gml CondFormattingGML.h cond_fmt_gml)
compile_gml(CondView.gml CondFormattingViewGML.h cond_fmt_view_gml)
compile_gml(csv_import.gml CSVImportGML.h csv_import_gml)
compile_gml(csv_export.gml CSVExportGML.h csv_export_gml)
compile_gml(select_format_page.gml FormatSelectionPageGML.h select_format_page_gml)

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
    ExportDialog.cpp
    HelpWindow.cpp
    ImportDialog.cpp
    JSIntegration.cpp
    Readers/XSV.cpp
    Spreadsheet.cpp
    SpreadsheetModel.cpp
    SpreadsheetView.cpp
    SpreadsheetWidget.cpp
    Workbook.cpp
    main.cpp
)

set(GENERATED_SOURCES
    CSVExportGML.h
    CSVImportGML.h
    FormatSelectionPageGML.h
)

serenity_app(Spreadsheet ICON app-spreadsheet)
target_link_libraries(Spreadsheet LibFileSystemAccessClient LibGUI LibJS LibMain LibWeb)
link_with_unicode_data(Spreadsheet)

serenity_test(Writers/Test/TestXSVWriter.cpp Spreadsheet)

serenity_test(Readers/Test/TestXSV.cpp Spreadsheet)
target_sources(TestXSV PRIVATE Readers/XSV.cpp)
