pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        maven { url = uri("https://maven.aliyun.com/repository/google") }      // Alibaba mirror - خیلی سریع در ایران
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/google_android") }  // Huawei mirror
        maven { url = uri("https://maven.google.com") }  // mirror رسمی گوگل (گاهی بهتر کار می‌کنه)

        // اصلی گوگل (آخر بذار تا اگه mirrorها کار نکردن، امتحان کنه)
        google()

        // برای artifactهایی که ممکنه به Maven Central منتقل شده باشن
        mavenCentral()

        // اختیاری: mirror عمومی برای بقیه کتابخونه‌ها
        maven { url = uri("https://maven.aliyun.com/repository/public") }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }      // Alibaba mirror - خیلی سریع در ایران
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/google_android") }  // Huawei mirror
        maven { url = uri("https://maven.google.com") }  // mirror رسمی گوگل (گاهی بهتر کار می‌کنه)
        maven { url = uri("https://jitpack.io") }  // mirror رسمی گوگل (گاهی بهتر کار می‌کنه)

        // اصلی گوگل (آخر بذار تا اگه mirrorها کار نکردن، امتحان کنه)
        google()

        // برای artifactهایی که ممکنه به Maven Central منتقل شده باشن
        mavenCentral()

        // اختیاری: mirror عمومی برای بقیه کتابخونه‌ها
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "solutions apps zebra"
include(":app")
 