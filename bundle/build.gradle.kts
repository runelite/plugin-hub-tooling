/*
 * Copyright (c) 2026 Abex
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
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

plugins {
	alias(libs.plugins.undercouch.download)
}

val distURL = "https://services.gradle.org/distributions/gradle-8.10-bin.zip"
val distSHA = "5b9c5eb3f9fc2c94abaea57d90bd78747ca117ddbbf96c859d3741181a12bf2a"

val wrapperZip = layout.buildDirectory.file("gradle.zip")
val downloadGradle = tasks.create<Download>("downloadGradle") {
	src(distURL)
	dest(wrapperZip)
}
val verifyGradle = tasks.create<Verify>("verifyGradle") {
	dependsOn(downloadGradle)
	src(wrapperZip)
	algorithm("SHA-256")
	checksum(distSHA)
}

val rtCopy = copySpec {
	from(provider { arrayOf(":apirecorder", ":package", ":upload").map {
		project(it).tasks.getByName<Jar>("shadowJar").archiveFile
	}})
	from(file("src/main/resources"))
	from(zipTree(wrapperZip)) {
		eachFile {
			relativePath.segments[0] = "gradle"
		}
		includeEmptyDirs = false
	}
}
var rtDeps = arrayOf(verifyGradle)

var tar = tasks.create<Tar>("tar") {
	archiveFileName = "bundle.tar"
	destinationDirectory = layout.buildDirectory.dir("distributions")

	dependsOn(rtDeps)
	with(rtCopy)
}
var rtDir = layout.buildDirectory.dir("test_runtime").get()
var testRTSync = tasks.create<Sync>("testRuntimeSync") {
	into(rtDir)

	dependsOn(rtDeps)
	with(rtCopy)
	from(file("src/test/resources"))
}

repositories {
	mavenLocal()
	maven {
		setUrl("https://repo.runelite.net")
		content {
			includeGroupByRegex("net\\.runelite.*")
		}
	}
	mavenCentral()
}

val runelite = configurations.create("runelite") {
	isCanBeConsumed = false
	isCanBeResolved = true
	isTransitive = false
}
dependencies {
	runelite("net.runelite:client:latest.release")
}

var rlVersion = tasks.create("rlVersion") {
	dependsOn(testRTSync, runelite)
	doLast {
		val version = runelite.incoming.resolutionResult.allComponents
			.mapNotNull { it.id as? ModuleComponentIdentifier }
			.single { it.group == "net.runelite" && it.module == "client" }
			.version

		rtDir.file("plugin-hub/runelite.version").asFile.writeText("$version\n")
	}
}

var testVerificationMetadata = tasks.create<Exec>("testVerificationMetadata") {
	val verTmplDir = rtDir.file("plugin-hub/package/verification-template")
	dependsOn(testRTSync, rlVersion)
	inputs.dir(verTmplDir)
	commandLine(
		rtDir.file("gradle/bin/gradle"),
		"--no-daemon",
		"--project-dir",
		verTmplDir,
		"--write-verification-metadata",
		"sha256",
		":verifyCore",
	)
	workingDir(rtDir)
}

var preparer = tasks.create<Exec>("preparer") {
	dependsOn(testRTSync, rlVersion)
	commandLine(rtDir.file("prepare.sh"))
	workingDir(rtDir)
}

val testRuntime by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}
var rtArtifact = artifacts.add("testRuntime", rtDir) {
	builtBy(testRTSync, preparer, rlVersion, testVerificationMetadata)
}

tasks.create("build") {
	dependsOn(rtArtifact, tar)
}