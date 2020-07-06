set(SOURCES
    main.cpp
)

serenity_bin(WebView)
target_link_libraries(WebView LibWeb LibGUI)
