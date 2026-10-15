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
package net.runelite.pluginhub.packager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.runelite.pluginhub.uploader.UploadConfiguration;
import okhttp3.HttpUrl;

public class DisableBroken
{
	public static void main(String []args) throws IOException
	{
		var phRepo = Path.of("../plugin-hub");

		var version = Files.readString(phRepo.resolve("runelite.version")).trim();

		try(var uc = new UploadConfiguration()
			.setClient(null)
			.setRuneLiteVersion(version)
			.setRoot(HttpUrl.get("https://repo.runelite.net/plugins")))
		{
			var mf = uc.getManifest(version, null);

			var early = Instant.now().plus(-14, ChronoUnit.DAYS).toEpochMilli() / 1000;

			for (var disp : mf.getDisplay())
			{
				if (disp.getBuildFailAt() != null && disp.getBuildFailAt() < early)
				{
					var path = phRepo.resolve("plugins").resolve(disp.getInternalName());
					var ptr = Files.readString(path, StandardCharsets.UTF_8);
					if (!ptr.endsWith("\n"))
					{
						ptr += "\n";
					}
					ptr += "disabled=unmaintained\n";
					Files.writeString(path, ptr, StandardCharsets.UTF_8);
				}
			}
		}
	}
}
