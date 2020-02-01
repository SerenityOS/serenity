set(SOURCES
	main.cpp
    LineEditor.cpp
    Parser.cpp
)

serenity_app(Shell)
target_link_libraries(Shell LibCore)
