set(SOURCES
    main.cpp
)

serenity_app(HelloWorld ICON app-hello-world)
target_link_libraries(HelloWorld LibGUI)
