set(SOURCES
    main.cpp
)

serenity_bin(HelloWorld)
target_link_libraries(HelloWorld LibGUI)
