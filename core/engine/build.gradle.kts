plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  `java-library`
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  api(project(":core:model"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
