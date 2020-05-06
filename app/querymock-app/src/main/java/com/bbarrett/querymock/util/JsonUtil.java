package com.bbarrett.querymock.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JsonUtil
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Object> parseJsonListFromFile(File file)
    {
        try
        {
            return Arrays.asList(objectMapper.readValue(file, Object[].class));
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return null;
        }
    }

    public static String getJsonString(Object object)
    {
        if (object == null)
            return null;
        else
        {
            try
            {
                return objectMapper.writeValueAsString(object);
            }
            catch (JsonProcessingException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static <T> T getObject(String json, Class<T> cls)
    {
        if (json == null)
            return null;
        else
        {
            try
            {
                return objectMapper.readValue(json, cls);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
                return null;
            }
        }
    }
}
