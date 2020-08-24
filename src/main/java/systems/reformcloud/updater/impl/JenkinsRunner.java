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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import systems.reformcloud.updater.Constants;
import systems.reformcloud.updater.config.RunnerConfiguration;
import systems.reformcloud.updater.http.HttpUtils;
import systems.reformcloud.updater.runner.Runner;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Runner.DefaultRunner
public class JenkinsRunner implements Runner {

    @Override
    public void run(@NotNull RunnerConfiguration configuration) {
        var jobUrl = configuration.getRunnerData().get("jobUrl").getAsString();
        var rev = configuration.getRunnerData().get("rev").getAsString();
        var pattern = Pattern.compile(configuration.getRunnerData().get("pattern").getAsString());

        HttpUtils.openConnection(jobUrl + "/" + rev + "/api/json", inputStream -> {
            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                List<Artifact> artifacts = Constants.GSON.get().fromJson(jsonObject.get("artifacts"), TypeToken.getParameterized(List.class, Artifact.class).getType());

                if (artifacts.isEmpty()) {
                    System.err.println("Unable to load artifacts of last jenkins build from " + jobUrl);
                    return;
                }

                for (Artifact artifact : artifacts) {
                    var matcher = pattern.matcher(artifact.fileName);
                    if (matcher.matches()) {
                        this.handleArtifactFound(artifact, matcher, jobUrl, rev, configuration.getTargetPath());
                        return;
                    }
                }

                System.err.println("Unable to find matching artifact for " + pattern + " @ " + jobUrl);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void handleArtifactFound(@NotNull Artifact artifact, @NotNull Matcher matcher, @NotNull String jobUrl, @NotNull String rev, @NotNull String target) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            list.add(matcher.group(i));
        }

        target = MessageFormat.format(target, list.toArray(Object[]::new));
        HttpUtils.download(jobUrl + "/" + rev + "/artifact/" + artifact.relativePath, target);
    }

    private static final class Artifact {

        private final String fileName;
        private final String relativePath;

        private Artifact(String fileName, String relativePath) {
            this.fileName = fileName;
            this.relativePath = relativePath;
        }
    }
}
