package io.chthonic.bigbox3d.network


external object window {
    val location: Location
}

external class Location {
    val hostname: String
}

external class URL {
    constructor(url: String)

    val pathname: String
}

@JsFun("encodeURIComponent")
external fun encodeURIComponent(value: String): String

private fun isDev(): Boolean =
    window.location.hostname == "localhost"

internal fun resolveExternalUrl(url: String): String {
    val pathname = URL(url).pathname
    return if (isDev()) {
        pathname
    } else {
        "/api/proxy?url=" + encodeURIComponent(url)
    }
}