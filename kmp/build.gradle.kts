
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.compose)
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.zhk.kmp"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 26

        // 显式启用Java编译支持
        // AGP9.0中，Android模块默认仅启用Kotlin支持，若需编译Java代码，需通过withJava()显式启用（替代旧版隐式支持）
        // withJava() 必须在android块内声明，否则Java文件会被AGP忽略
        withJava() // enable java compilation support

        //传统Android开发: androidTest目录专门存放Instrumented Tests（运行在设备上的测试）。test目录存放Unit Tests（运行在JVM上的测试）
        //KMP早期: KMP引入了androidAndroidTest来指代设备测试，而androidTest有时会被混淆或用于指代JVM测试，或者干脆不存在
        withHostTestBuilder {
            sourceSetTreeName = "test"
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            // 核心：指定 Kotlin/Java 编译的目标 JVM 版本（强类型枚举，避免字符串写错）
            // 为什么用 JvmTarget.JVM_17：AGP 9.0+ 与 Kotlin Gradle 插件深度整合，JvmTarget 是 Kotlin 官方定义的枚举，
            // 比 JavaVersion 更贴合 Kotlin 编译逻辑，避免 “Java 版本” 和 “Kotlin 编译目标版本” 不一致的问题。
            // 老版本中需要使用compileOptions/kotlinOptions两个配置分别指定JVM版本
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)

            // 扩展：添加 Kotlin 编译器参数（旧版需单独配置）
            // -Xjvm-default=all：让 Kotlin 接口的默认方法生成符合 JVM 17 规范的字节码
            // freeCompilerArgs 的作用：直接传递 Kotlin 编译器参数，比如 -Xjvm-default=all 解决 KMP 中接口默认方法的跨平台兼容问题，
            // 旧版 compileOptions 完全做不到这一点。
            freeCompilerArgs.add("-Xjvm-default=all")
        }

    }

//    java {
//        toolchain {
//            languageVersion.set(JavaLanguageVersion.of(17))
//        }
//    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "kmpKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.ui.graphics)
                implementation(libs.androidx.compose.ui.tooling.preview)
                implementation(libs.androidx.compose.material3)
                implementation(libs.junit)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }

        //默认行为: sourceSets 块中，Kotlin Gradle Plugin (KGP) 默认只会为常见的、预定义的源集创建配置入口（如 commonMain, commonTest,
        // androidMain）。对于像 androidDeviceTest 这样由新 DSL 动态生成或相对较新的源集，sourceSets DSL 可能不会自动生成一个类型安全的访问器
        // （即你不能直接写 androidDeviceTest { ... }）。
        //动态获取: 使用 getByName("androidDeviceTest") 是一种通用的方式，通过字符串名称动态获取该源集的配置对象。
        // 这确保了即使 IDE 或 Gradle 没有为这个特定的源集生成扩展属性，你仍然可以配置它。
        //目的: 这样做的目的是为了给 Instrumented Tests（设备测试） 添加特定的依赖。这些依赖（如 androidx.test.runner, espresso）
        // 只能在 Android 设备上运行，必须与主机测试（androidHostTest）和主代码（androidMain）的依赖隔离开来，以避免冲突和减小包体积。
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}
