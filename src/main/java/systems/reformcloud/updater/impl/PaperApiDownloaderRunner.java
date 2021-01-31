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

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import systems.reformcloud.updater.Constants;
import systems.reformcloud.updater.config.RunnerConfiguration;
import systems.reformcloud.updater.http.HttpUtils;
import systems.reformcloud.updater.runner.Runner;

import java.lang.reflect.Type;
import java.util.Set;

public class PaperApiDownloaderRunner implements Runner {

  private static final String VERSION_LIST_URL = "https://papermc.io/api/v2/projects/%s/versions/%s";
  private static final String DOWNLOAD_URL = "https://papermc.io/api/v2/projects/%s/versions/%s/builds/%d/downloads/%s-%s-%d.jar";
  private static final Type INT_SET_TYPE = TypeToken.getParameterized(Set.class, Integer.class).getType();

  @Override
  public void run(@NotNull RunnerConfiguration configuration) {
    var versionGroup = configuration.getRunnerData().get("versionGroup").getAsString();
    var projectName = configuration.getRunnerData().get("projectName").getAsString();

    var object = HttpUtils.makeRequest(String.format(VERSION_LIST_URL, projectName, versionGroup));
    if (object != null && object.has("builds")) {
      Set<Integer> builds = Constants.GSON.get().fromJson(object.get("builds"), INT_SET_TYPE);
      builds.stream().reduce(Math::max).ifPresent(buildNumber -> HttpUtils.download(
        String.format(DOWNLOAD_URL, projectName, versionGroup, buildNumber, projectName, versionGroup, buildNumber),
        configuration.getTargetPath()
      ));
    }
  }
}
