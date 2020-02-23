import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//tag::plugins[]
plugins {
    //tag::ignore_plugins[]
    id("se.patrikerdes.use-latest-versions") version "0.2.13"
    id("com.github.ben-manes.versions") version "0.28.0"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
    kotlin("plugin.jpa") version "1.3.61"

    //end::ignore_plugins[]
    id("org.springframework.boot") version "2.2.4.RELEASE" // <1>
    id("io.spring.dependency-management") version "1.0.9.RELEASE" // <2>
}

//end::plugins[]

group = "com.secrets_as_a_service.blog"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "Hoxton.SR1"

//tag::dependencies[]
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") // <3>
    //tag::ignore_dependencies[]
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-vault-config-databases")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.7.8")
    //end::ignore_dependencies[]
    runtimeOnly("org.postgresql:postgresql") // <4>
}
//end::dependencies[]

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
