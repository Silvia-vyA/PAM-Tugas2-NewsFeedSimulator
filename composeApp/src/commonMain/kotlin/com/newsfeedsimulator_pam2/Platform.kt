package com.newsfeedsimulator_pam2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform