package com.emperador.radio2.core.error

enum class Error(var type: String) {

    CACHE_EMPTY("CACHE_EMPTY"),
    UNEXPECTED("UNEXPECTED"),
    SERVER_404("Not Found URL SERVER_404"),
    SERVER_500("Not Found URL SERVER_500"),
    NO_INTERNET("No internet connection"),
    UNIMPLEMENTED("Methodo sin implementar"),
    UNIMPLEMENTED_NETWORK_ERROR("Error de conexion sin controlar"),
    NETWORK_ERROR_NULL("NETWORK_ERROR_NULL"),

}

