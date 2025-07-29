package org.macasteglione.keepsafe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform