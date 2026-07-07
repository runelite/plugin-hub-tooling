/*
 * Copyright (c) 2021 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
plugins {
	java
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(11)
}

repositories {
	maven {
		url = uri("https://repo.gradle.org/gradle/libs-releases-remote-cache/")
	}
	mavenCentral()
}

val testRuntime by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	implementation(libs.gradle.tooling.api)
	implementation(libs.slf4j.simple)
	implementation(libs.findbugs.jsr305)
	implementation(libs.guava)
	implementation(libs.asm)
	implementation(libs.okhttp)
	implementation(libs.gson)
	implementation(project(":upload"))
	implementation(project(":apirecorder"))

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)

	testImplementation(libs.junit)
	testImplementation(libs.okhttp.mockwebserver)

	testRuntime(project(path = ":bundle", configuration = "testRuntime"))
}

tasks.named<Jar>("jar") {
	manifest {
		attributes("Main-Class" to "net.runelite.pluginhub.packager.Packager")
	}
}

tasks.named<Test>("test") {
	dependsOn(testRuntime)
	workingDir = testRuntime.singleFile
	inputs.file(file("../create_new_plugin.py"))
	inputs.dir(file("../templateplugin"))
	systemProperty("pluginhub.test.createNewPlugin", file("../create_new_plugin.py"))
}
