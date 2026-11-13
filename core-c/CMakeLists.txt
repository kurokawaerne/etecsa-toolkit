cmake_minimum_required(VERSION 3.16)
project(toolkit_c C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

add_library(toolkit_c STATIC src/toolkit_c.c)
target_include_directories(toolkit_c PUBLIC src)
target_compile_options(toolkit_c PRIVATE -Wall -Wextra)

add_executable(toolkit_c_test src/test_main.c)
target_link_libraries(toolkit_c_test PRIVATE toolkit_c m)

enable_testing()
add_test(NAME toolkit_c_test COMMAND toolkit_c_test)
