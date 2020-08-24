/*
 * MIT License
 *
 * Copyright (c) ReformCloud-Team
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package systems.reformcloud.updater.impl;

import org.jetbrains.annotations.NotNull;
import systems.reformcloud.updater.config.RunnerConfiguration;
import systems.reformcloud.updater.http.HttpUtils;
import systems.reformcloud.updater.io.FileUtils;
import systems.reformcloud.updater.runner.Runner;

import java.io.IOException;
import java.nio.file.Path;

@Runner.DefaultRunner
public class BuildToolsRunner implements Runner {

    @Override
    public void run(@NotNull RunnerConfiguration configuration) {
        var executingPath = Path.of(BuildToolsRunner.class.getSimpleName() + "-" + System.nanoTime());
        var downloadUrl = configuration.getRunnerData().get("downloadUrl").getAsString();
        var copy = configuration.getRunnerData().get("copy").getAsString();
        var rev = configuration.getRunnerData().get("rev").getAsString();

        HttpUtils.download(downloadUrl, executingPath.resolve("run.jar"));

        try {
            var process = new ProcessBuilder()
                    .command("java", "-jar", "run.jar", "--rev", rev)
                    .directory(executingPath.toFile())
                    .inheritIO()
                    .start();
            process.waitFor();
            process.destroyForcibly();

            FileUtils.copy(executingPath.resolve(copy), configuration.getTargetPath());
            FileUtils.delete(executingPath);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
