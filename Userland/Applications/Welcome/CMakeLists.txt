set(SOURCES
    BackgroundWidget.cpp
    main.cpp
    TextWidget.cpp
    UnuncheckableButton.cpp
)

serenity_bin(Welcome)
target_link_libraries(Welcome LibGUI)
