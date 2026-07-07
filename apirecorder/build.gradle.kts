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
	jacoco
}

repositories {
	maven {
		url = uri("https://repo.runelite.net")
	}
	mavenCentral()
}

val testCore by sourceSets.creating
val testPlugin by sourceSets.creating {
	compileClasspath += testCore.output
}

dependencies {
	implementation(libs.slf4j.simple)
	implementation(libs.findbugs.jsr305)
	implementation(libs.guava)
	implementation(libs.asm)
	implementation(libs.gson)

	testImplementation(libs.junit)

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
}

tasks.withType<JavaCompile>().configureEach {
	// we can't use -release here because we need to --add-exports for the javac
	// internal apis
	sourceCompatibility = "11"
	targetCompatibility = "11"
}

tasks.named<JavaCompile>("compileJava") {
	options.compilerArgs.addAll(listOf(
		"--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
		"--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
	))
}

val testCoreJar = tasks.register<Jar>("testCoreJar") {
	archiveBaseName.set("test-core")
	from(sourceSets.getByName("testCore").output)
}

val testClassApiFile = project.layout.buildDirectory.file("testCoreApi").get().asFile

val testCoreApi = tasks.register<JavaExec>("testCoreApi") {
	inputs.file(testCoreJar.get().archiveFile)
	outputs.file(testClassApiFile)
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("net.runelite.pluginhub.apirecorder.ClassRecorder")
	args(testClassApiFile, testCoreJar.get().archiveFile.get().asFile)
}

tasks.named<Test>("test") {
	inputs.files(testCoreApi.get().outputs)
	inputs.files(testPlugin.allSource)

	jvmArgs(
		"--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
		"--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
	)

	systemProperties(mapOf(
		"runelite.apirecorder.test.classpath" to testPlugin.compileClasspath.files.joinToString(File.pathSeparator),
		"runelite.apirecorder.test.sourcepath" to testPlugin.allJava.files.joinToString(File.pathSeparator),
	))
}

tasks.named<JacocoReport>("jacocoTestReport") {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}
