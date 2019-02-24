plugins {
    kotlin("jvm")
}

kotlinProject()

repositories {
    jcenter()
}

dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
    implementation("joda-time:joda-time:2.9.9")
}