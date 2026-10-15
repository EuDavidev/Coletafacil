plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.register<JavaExec>("verifyCore") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("br.com.coletafacil.core.CoreTests")
    args(layout.buildDirectory.dir("test-exports").get().asFile.absolutePath)
}
tasks.check { dependsOn("verifyCore") }
