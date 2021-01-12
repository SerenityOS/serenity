set(SOURCES
    ClipboardHistoryModel.cpp
    main.cpp
)

serenity_app(ClipboardHistory.MenuApplet ICON clipboard)
target_link_libraries(ClipboardHistory.MenuApplet LibGUI LibCore LibGfx)
