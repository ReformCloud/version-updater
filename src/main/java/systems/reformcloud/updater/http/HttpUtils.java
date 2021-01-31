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
package systems.reformcloud.updater.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.reformcloud.updater.io.FileUtils;
import systems.reformcloud.updater.logger.LoggingUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class HttpUtils {

  private HttpUtils() {
    throw new UnsupportedOperationException();
  }

  public static void openConnection(@NotNull String url, @NotNull Consumer<InputStream> handler) {
    try {
      var httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
      httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
      httpURLConnection.setDoOutput(false);
      httpURLConnection.setUseCaches(false);
      httpURLConnection.connect();

      try (final InputStream inputStream = httpURLConnection.getInputStream()) {
        handler.accept(inputStream);
      }

      httpURLConnection.disconnect();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public static void download(@NotNull String url, @NotNull String target) {
    download(url, Path.of(target));
  }

  public static void download(@NotNull String url, @NotNull Path target) {
    FileUtils.delete(target);

    if (target.getParent() != null && Files.notExists(target.getParent())) {
      LoggingUtils.executeSecretly(() -> Files.createDirectories(target.getParent()));
    }

    HttpUtils.openConnection(url, inputStream -> {
      try (var out = Files.newOutputStream(target)) {
        inputStream.transferTo(out);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    });
  }

  @Nullable
  public static JsonObject makeRequest(@NotNull String apiUrl) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
      connection.setRequestProperty("accepts", "application/json");
      connection.connect();
      if (connection.getResponseCode() == 200) {
        try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
          return JsonParser.parseReader(reader).getAsJsonObject();
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return null;
  }
}
