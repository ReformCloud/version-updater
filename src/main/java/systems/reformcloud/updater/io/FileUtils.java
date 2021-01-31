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
package systems.reformcloud.updater.io;

import org.jetbrains.annotations.NotNull;
import systems.reformcloud.updater.logger.LoggingUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtils {

  private FileUtils() {
    throw new UnsupportedOperationException();
  }

  public static void delete(@NotNull Path directoryPath) {
    if (Files.notExists(directoryPath)) {
      return;
    }

    if (Files.isDirectory(directoryPath)) {
      deleteDirectory0(directoryPath);
    } else {
      deleteFile0(directoryPath);
    }
  }

  public static void copy(@NotNull Path file, @NotNull String target) {
    if (Files.notExists(file) || Files.isDirectory(file)) {
      return;
    }

    Path targetPath = Path.of(target);
    if (Files.exists(targetPath)) {
      delete(targetPath);
    }

    if (targetPath.getParent() != null && Files.notExists(targetPath.getParent())) {
      LoggingUtils.executeSecretly(() -> Files.createDirectories(targetPath.getParent()));
    }

    try (var out = Files.newOutputStream(targetPath)) {
      Files.copy(file, out);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  private static void deleteFile0(@NotNull Path path) {
    LoggingUtils.executeSecretly(() -> Files.deleteIfExists(path));
  }

  private static void deleteDirectory0(@NotNull Path directoryPath) {
    LoggingUtils.executeSecretly(() -> {
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)) {
        for (Path path : directoryStream) {
          if (Files.isDirectory(path)) {
            deleteDirectory0(path);
          } else {
            deleteFile0(path);
          }
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }

      deleteFile0(directoryPath);
    });
  }
}
