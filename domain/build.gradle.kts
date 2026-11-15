plugins {
    id("org.jetbrains.kotlin.jvm")
}
dependencies {
    implementation(project(":core-common"))
    testImplementation(kotlin("test"))
}
kotlin { jvmToolchain(17) }
