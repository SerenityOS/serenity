set(SOURCES
    HelpWindow.cpp
    Spreadsheet.cpp
    SpreadsheetModel.cpp
    SpreadsheetView.cpp
    SpreadsheetWidget.cpp
    main.cpp
)

serenity_bin(Spreadsheet)
target_link_libraries(Spreadsheet LibGUI LibJS LibWeb)
