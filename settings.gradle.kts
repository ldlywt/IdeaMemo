pluginManagement {
    repositories {
        maven ( "https://maven.aliyun.com/repository/gradle-plugin/" ) // Gradle 插件镜像
        maven("https://www.jitpack.io/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven ( "https://maven.aliyun.com/repository/public/" ) // 阿里云公共仓库
        maven ("https://maven.aliyun.com/repository/google/" ) // Google 镜像
        maven("https://jitpack.io")
        google()
        mavenCentral()
    }
}

rootProject.name = "Note"
include(":app")
