import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  idea
  kotlin("jvm") version "1.3.72" // Keep in sync with README
}

allprojects {
  repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/mipt-npm/dev")
    maven("https://dl.bintray.com/hotkeytlt/maven")
//    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

idea.module {
  excludeDirs.add(file("latex"))
  isDownloadJavadoc = true
  isDownloadSources = true
}

subprojects {
  group = "edu.umontreal"
  version = "0.3.1"

  apply(plugin = "org.jetbrains.kotlin.jvm")

  tasks {
    val jvmTarget = VERSION_1_8.toString()
    compileKotlin {
      kotlinOptions.jvmTarget = jvmTarget
      kotlinOptions.freeCompilerArgs += "-XXLanguage:+NewInference"
    }
    compileTestKotlin {
      kotlinOptions.jvmTarget = jvmTarget
    }
    test {
      minHeapSize = "1024m"
      maxHeapSize = "4096m"
      useJUnitPlatform()
      testLogging {
        events = setOf(FAILED, PASSED, SKIPPED, STANDARD_OUT)
        exceptionFormat = FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
      }
    }
  }
}

val installPathLocal = "${System.getProperty("user.home")}/.jupyter_kotlin/libraries"

tasks.register<Copy>("jupyterInstall") {
  val installPath = findProperty("ath") ?: installPathLocal
  doFirst { mkdir(installPath) }
  from(file("kotlingrad.json"))
  into(installPath)
  doLast { logger.info("Kotlin∇ notebook was installed in: $installPath") }
}