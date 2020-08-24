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
package systems.reformcloud.updater.config;

import com.github.derrop.documents.Documents;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.reformcloud.updater.runner.Runner;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

public class VersionUpdaterConfiguration {

    public static void loadAndRun(@NotNull ExecutorService executor) {
        var configPath = Path.of("config.json");
        if (Files.notExists(configPath)) {
            Documents.newDocument().append("runners", Arrays.asList(
                    new RunnerConfiguration(
                            "systems.reformcloud.updater.impl.BuildToolsRunner",
                            "final/spigot-1.16.2.jar",
                            createBuildToolsRunnerData()
                    ),
                    new RunnerConfiguration(
                            "systems.reformcloud.updater.impl.PaperClipRunner",
                            "final/paper-1.16.1.jar",
                            createPaperClipRunnerData()
                    ),
                    new RunnerConfiguration(
                            "systems.reformcloud.updater.impl.JenkinsRunner",
                            "final/velocity-{0}.jar",
                            createJenkinsRunnerData()
                    )
            )).json().write(configPath);

            System.out.println("Please fill the settings and restart");
            return;
        }

        Collection<RunnerConfiguration> configurations = Documents.jsonStorage().read(configPath).get("runners", TypeToken.getParameterized(Collection.class, RunnerConfiguration.class).getType());
        System.out.println("Loaded " + configurations.size() + " configurations...");

        for (var configuration : configurations) {
            Runner runner = tryLoadRunner(configuration.getRunnerClass());
            if (runner == null) {
                System.err.println("Unable to load runner: " + configuration.getRunnerClass());
                continue;
            }

            executor.submit(() -> {
                System.out.println("Executing runner task: " + configuration.getRunnerClass());
                runner.run(configuration);
            });
        }
    }

    private static @NotNull JsonObject createBuildToolsRunnerData() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("downloadUrl", "https://hub.spigotmc.org/jenkins/job/BuildTools/115/artifact/target/BuildTools.jar");
        jsonObject.addProperty("copy", "spigot-1.16.2.jar");
        jsonObject.addProperty("rev", "1.16.2");

        return jsonObject;
    }

    private static @NotNull JsonObject createPaperClipRunnerData() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("downloadUrl", "https://papermc.io/ci/job/Paper-1.16/lastSuccessfulBuild/artifact/paperclip.jar");
        jsonObject.addProperty("copy", "cache/patched_1.16.1.jar");

        return jsonObject;
    }

    private static @NotNull JsonObject createJenkinsRunnerData() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("jobUrl", "https://ci.velocitypowered.com/job/velocity-1.1.0");
        jsonObject.addProperty("rev", "lastSuccessfulBuild");
        jsonObject.addProperty("pattern", "velocity-proxy-(.*)-SNAPSHOT-all.jar");

        return jsonObject;
    }

    private static @Nullable Runner tryLoadRunner(@NotNull String clazz) {
        try {
            Class<?> runnerClass = Class.forName(clazz);
            return (Runner) runnerClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException | ClassCastException exception) {
            return null;
        }
    }
}
