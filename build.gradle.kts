import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

val platformVersion = providers.gradleProperty("platformVersion")
val webStormEapPath = providers.gradleProperty("webStormEapPath")

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea(platformVersion)
        bundledPlugin("Git4Idea")
        bundledPlugin("org.jetbrains.plugins.github")
        testFramework(TestFrameworkType.Platform)
    }
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
}
