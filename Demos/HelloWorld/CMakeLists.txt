set(SOURCES
    main.cpp
)

add_executable(HelloWorld ${SOURCES})
target_link_libraries(HelloWorld LibGUI)
