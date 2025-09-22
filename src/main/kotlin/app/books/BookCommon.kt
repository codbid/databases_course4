package com.example.app.books

enum class BookCopyStatus(val status: String) {
    AVAILABLE("AVAILABLE"),
    RESERVED("RESERVED"),
    LOANED("LOANED"),
    UNAVAILABLE("UNAVAILABLE");
}