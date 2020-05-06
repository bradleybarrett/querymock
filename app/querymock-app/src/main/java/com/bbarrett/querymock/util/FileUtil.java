package com.bbarrett.querymock.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil
{
    public static List<File> getFilesFromDirectory(String directory)
    {
        try
        {
            return Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return new ArrayList<>();
        }
    }
}
