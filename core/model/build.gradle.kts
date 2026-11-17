plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  `java-library`
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
}
