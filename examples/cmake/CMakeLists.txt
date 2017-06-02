project(hello)
add_executable(hello src/hello.c)
install(TARGETS hello DESTINATION bin)
