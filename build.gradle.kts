import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

val platformVersion = providers.gradleProperty("platformVersion")
val webStormEapPath = providers.gradleProperty("webStormEapPath")
val e2eIde = providers.gradleProperty("e2eIde").orElse(platformVersion)

kotlin {
    jvmToolchain(21)
}

// The Starter and Driver libraries are built for Java 25, so the end-to-end tests compile and run on it.
val integrationTestJava = JavaLanguageVersion.of(25)

sourceSets {
    create("integrationTest")
}

tasks.named<KotlinCompile>("compileIntegrationTestKotlin") {
    kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor { languageVersion = integrationTestJava })
    compilerOptions.jvmTarget = JvmTarget.fromTarget(integrationTestJava.toString())
}

tasks.named<JavaCompile>("compileIntegrationTestJava") {
    javaCompiler = javaToolchains.compilerFor { languageVersion = integrationTestJava }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea(platformVersion)
        bundledPlugin("Git4Idea")
        bundledPlugin("org.jetbrains.plugins.github")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation")
    }

    "integrationTestImplementation"(kotlin("stdlib"))
    "integrationTestImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
    "integrationTestImplementation"(platform("org.junit:junit-bom:5.14.4"))
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter")
    "integrationTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    pluginVerification {
        failureLevel = listOf(FailureLevel.COMPATIBILITY_PROBLEMS, FailureLevel.INVALID_PLUGIN)
        ides {
            create(IntelliJPlatformType.WebStorm, platformVersion)
            create(IntelliJPlatformType.PhpStorm, platformVersion)
            create(IntelliJPlatformType.IntellijIdea, platformVersion)
            if (webStormEapPath.isPresent) {
                local(webStormEapPath)
            }
            latest {
                types = listOf(IntelliJPlatformType.WebStorm)
                channels = listOf(ProductRelease.Channel.EAP)
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runWebStorm") {
            type = IntelliJPlatformType.WebStorm
            version = platformVersion
        }
        register("runPhpStorm") {
            type = IntelliJPlatformType.PhpStorm
            version = platformVersion
        }
        if (webStormEapPath.isPresent) {
            register("runWebStormEap") {
                localPath = file(webStormEapPath)
            }
        }
    }
    testIdeUi {
        register("integrationTest") {
            task {
                val integrationTestSourceSet = sourceSets["integrationTest"]
                testClassesDirs = integrationTestSourceSet.output.classesDirs
                classpath = integrationTestSourceSet.runtimeClasspath
                useJUnitPlatform { includeEngines("junit-jupiter") }
                javaLauncher = javaToolchains.launcherFor { languageVersion = integrationTestJava }
                systemProperty("reviewQueue.e2e.ide", e2eIde.get())
            }
        }
    }
}
