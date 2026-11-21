plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

group = "edu.hse.concurrency.metrics"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jmh = sourceSets.create("jmh") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    add(jmh.implementationConfigurationName, "org.openjdk.jmh:jmh-core:1.37")
    add(jmh.annotationProcessorConfigurationName, "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    dependsOn(jmh.classesTaskName)
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    classpath = jmh.runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    args("CounterBenchmark", "-t", providers.gradleProperty("jmhThreads").orElse("1").get(), "-foe", "true")
}
