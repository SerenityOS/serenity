set(SOURCES
    main.cpp
    BoardListModel.cpp
    ThreadCatalogModel.cpp
)

serenity_app(ChanViewer)
target_link_libraries(ChanViewer LibGUI)
