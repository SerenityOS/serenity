set(SOURCES
    main.cpp
    TextWidget.cpp
)

serenity_app(Welcome)
target_link_libraries(Welcome LibGUI)
