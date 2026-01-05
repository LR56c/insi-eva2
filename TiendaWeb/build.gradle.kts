plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "cl.iplacex"
version = "0.0.1-SNAPSHOT"
description = "Tienda Web TechNova SpA"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    implementation("com.sun.xml.bind:jaxb-ri:4.0.5")


	implementation("org.apache.activemq:artemis-jakarta-client:2.44.0")
	implementation("org.apache.activemq:artemis-commons:2.44.0")
	implementation("com.google.code.gson:gson:2.13.2")

	implementation("org.springframework.boot:spring-boot-starter-artemis:4.0.1")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
